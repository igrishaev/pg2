package org.pg;

import org.pg.error.PGError;
import org.pg.util.TryLock;

import java.util.Deque;
import java.util.UUID;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.HashMap;

public final class Pool implements AutoCloseable {

    private final Config config;
    private final Map<UUID, Connection> connsUsed;
    private final Deque<Connection> connsFree;
    private boolean isClosed = false;
    private final static System.Logger logger = System.getLogger(Pool.class.getCanonicalName());
    private final TryLock lock = new TryLock();

    private Pool (final Config config) {
        this.config = config;
        this.connsUsed = new HashMap<>(config.poolMaxSize());
        this.connsFree = new ArrayDeque<>(config.poolMaxSize());
    }

    public static Pool create (final String host,
                               final int port,
                               final String user,
                               final String password,
                               final String database)
    {
        return create(Config.builder(user, database)
                .host(host)
                .port(port)
                .password(password)
                .build());
    }

    public static Pool create (final Config config) {
        final Pool pool = new Pool(config);
        pool._initiate();
        return pool;
    }

    private void _initiate () {
        for (int i = 0; i < config.poolMinSize(); i++) {
            spawnConnection();
        }
    }

    private boolean isExpired (final Connection conn) {
        return System.currentTimeMillis() - conn.getCreatedAt() > config.poolLifetimeMs();
    }

    private void addUsed (final Connection conn) {
        connsUsed.put(conn.getId(), conn);
    }

    private void removeUsed (final Connection conn) {
        connsUsed.remove(conn.getId());
    }

    private boolean isUsed (final Connection conn) {
        return connsUsed.containsKey(conn.getId());
    }

    @SuppressWarnings("unused")
    public Connection borrowConnection () {
        try (TryLock ignored = lock.get()) {
            return _borrowConnection_unlocked();
        }
    }

    @SuppressWarnings("unused")
    private Connection _borrowConnection_unlocked () {

        int gap;
        Connection conn;

        if (isClosed()) {
            throw new PGError("Cannot get a connection: the pool has been closed");
        }

        final int maxSize = config.poolMaxSize();

        while (true) {
            conn = connsFree.poll();
            if (conn == null) { // No free connections

                // fill up missing connections
                gap = config.poolMinSize() - connsUsed.size();
                if (gap > 0) {
                    for (var i = 0; i < gap; i++) {
                        conn = spawnConnection();
                    }
                    addUsed(conn);
                    return conn;
                }

                if (connsUsed.size() < maxSize) {
                    conn = spawnConnection();
                    addUsed(conn);
                    return conn;
             }

                else {
                    final String message = String.format(
                            "The pool is exhausted: %s out of %s connections are in use",
                            connsUsed.size(),
                            maxSize
                    );
                    logger.log(config.logLevel(), message);
                    throw new PGError(message);
                }
            }
            if (conn.isClosed()) {
                final String message = String.format(
                        "Connection %s has been already closed, ignoring",
                        conn.getId()
                );
                logger.log(config.logLevel(), message);
                continue;
            }
            if (isExpired(conn)) {
                utilizeConnection(conn);
                continue;
            }
            else {
                addUsed(conn);
                return conn;
            }
        }
    }

    private void utilizeConnection(final Connection conn) {
        conn.close();
        logger.log(
                config.logLevel(),
                "the connection {0} has been closed, free: {1}, used: {2}, max: {3}",
                conn.getId(),
                connsFree.size(),
                connsUsed.size(),
                config.poolMaxSize()
        );
    }

    private Connection spawnConnection() {
        final Connection conn = Connection.connect(config);
        offerConnection(conn);
        logger.log(
                config.logLevel(),
                "connection {0} has been created, free: {1}, used: {2}, max: {3}",
                conn.getId(),
                connsFree.size(),
                connsUsed.size(),
                config.poolMaxSize()
        );
        return conn;
    }

    @SuppressWarnings("unused")
    public void returnConnection (final Connection conn) {
        returnConnection(conn, false);
    }

    public void returnConnection (final Connection conn, final boolean forceClose) {
        try (TryLock ignored = lock.get()) {
            _returnConnection_unlocked(conn, forceClose);
        }
    }

    private void offerConnection (final Connection conn) {
        if (!connsFree.offer(conn)) {
            throw new PGError("could not return connection %s to the pool", conn.getId());
        }
    }

    private void _returnConnection_unlocked (final Connection conn, final boolean forceClose) {

        if (!isUsed(conn)) {
            throw new PGError("connection %s doesn't belong to the pool", conn.getId());
        }

        removeUsed(conn);

        if (isClosed()) {
            utilizeConnection(conn);
            return;
        }

        if (conn.isClosed()) {
            logger.log(
                    config.logLevel(),
                    "connection {0} has bin closed, ignoring",
                    conn.getId()
            );
            return;
        }

        if (forceClose) {
            utilizeConnection(conn);
            return;
        }

        if (isExpired(conn)) {
            logger.log(
                    config.logLevel(),
                    "connection {0} has expired, closing",
                    conn.getId()
            );
            utilizeConnection(conn);
            return;
        }

        if (conn.isTxError()) {
            logger.log(
                    config.logLevel(),
                    "connection {0} is in error state, rolling back",
                    conn.getId()
            );
            conn.rollback();
            utilizeConnection(conn);
            return;
        }

        if (conn.isTransaction()) {
            logger.log(
                    config.logLevel(),
                    "connection {0} is in transaction, rolling back",
                    conn.getId()
            );
            conn.rollback();
        }

        offerConnection(conn);

        logger.log(
                config.logLevel(),
                "connection {0} has been returned to the pool",
                conn.getId()
        );
    }

    public void close () {
        try (TryLock ignored = lock.get()) {
            _close_unlocked();
        }
    }

    private void _close_unlocked () {
        for (final Connection conn: connsFree) {
            conn.close();
        }
        for (final Connection conn: connsUsed.values()) {
            conn.close();
        }
        isClosed = true;
    }

    public boolean isClosed() {
        try (TryLock ignored = lock.get()) {
            return isClosed;
        }
    }

    @SuppressWarnings("unused")
    public int usedCount () {
        try (TryLock ignored = lock.get()) {
            return connsUsed.size();
        }
    }

    @SuppressWarnings("unused")
    public int freeCount () {
        try (TryLock ignored = lock.get()) {
            return connsFree.size();
        }
    }

    public String toString () {
        try (TryLock ignored = lock.get()) {
            return String.format(
                    "<PG pool, min: %s, max: %s, lifetime: %s>",
                    config.poolMinSize(),
                    config.poolMaxSize(),
                    config.poolLifetimeMs()
            );
        }
    }

}
