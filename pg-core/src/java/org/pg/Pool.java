package org.pg;

import org.pg.error.PGError;
import org.pg.util.TryLock;
import java.util.*;

public final class Pool implements AutoCloseable {

    private final UUID id;
    private final Config config;
    private final Map<UUID, Connection> connsUsed;
    private final ArrayDeque<Connection> connsFree;
    private boolean isClosed = false;
    private final static System.Logger logger = System.getLogger(Pool.class.getCanonicalName());
    private final TryLock lock = new TryLock();
    
    @Override
    public boolean equals (Object other) {
        return other instanceof Pool && id.equals(((Pool) other).id);
    }

    @Override
    public int hashCode () {
        return this.id.hashCode();
    }

    public void replenishConnections() {
        Connection conn;
        logger.log(System.Logger.Level.DEBUG, "Start connection replenishment task, pool: {0}", id);
        final int gap = config.poolMinSize() - connsUsed.size() - connsFree.size();
        try (TryLock ignored = lock.get()) {
            if (gap > 0) {
                for (var i = 0; i < gap; i++) {
                    conn = spawnConnection();
                    addFree(conn);
                }
            }
        }
    }

    private Pool (final Config config) {
        final int size = config.poolMaxSize();
        this.id = UUID.randomUUID();
        this.config = config;
        this.connsUsed = new HashMap<>(size);
        this.connsFree = new ArrayDeque<>(size);
    }

    @SuppressWarnings("unused")
    public UUID getId() {
        return id;
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
        return new Pool(config).initiate();
    }

    private Pool initiate() {
        replenishConnections();
        return this;
    }

    private boolean isExpired (final Connection conn) {
        return System.currentTimeMillis() - conn.getCreatedAt() > config.poolExpireThresholdMs();
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
        Connection conn;
        int attempt = 0;
        while (true) {
            try (TryLock ignored = lock.get()) {
                conn = borrowConnectionLocked();
            }
            if (conn == null) {
                attempt += 1;
                if (attempt > config.poolBorrowConnAttempts()) {
                    throw new PGError("Pool %s is exhausted! min: %s, max: %s, free: %s, used: %s, attempt: %s, timeout: %s",
                            id,
                            config.poolMinSize(),
                            config.poolMaxSize(),
                            freeCount(),
                            usedCount(),
                            attempt,
                            config.poolBorrowConnTimeoutMs()
                    );
                }
                else {
                    try {
                        Thread.sleep(config.poolBorrowConnTimeoutMs());
                    } catch (InterruptedException e) {
                        throw new PGError("Connection polling interrupted! Pool: %s", id);
                    }
                }
            }
            else {
                return conn;
            }
        }
    }

    private Connection borrowConnectionLocked() {
        final Connection conn = getOrSpawnConnection();
        if (conn == null) {
            return null;
        }
        else {
            addUsed(conn);
        }
        return conn;
    }

    private Connection getOrSpawnConnection() {

        if (isClosed()) {
            throw new PGError("Cannot get a connection: the pool has been closed");
        }

        // try to get from the queue without waiting
        final Connection conn = connsFree.poll();

        // if found...
        if (conn != null) {

            // if expired, close and return null
            if (isExpired(conn)) {
                logger.log(System.Logger.Level.DEBUG, "Connection {0} has been expired, closing. Pool: {1}", conn.getId(),  this.id);
                closeConnection(conn);
                return null;
            }

            // not found
            return conn;
        }

        // no free connections. If possible, create a new one
        if (connsUsed.size() < config.poolMaxSize()) {
            return spawnConnection();
        }

        return null;
    }

    private void closeConnection(final Connection conn) {
        conn.close();
        logger.log(System.Logger.Level.DEBUG, "Connection {0} has been closed, pool: {1}", conn.getId(), this.id);
    }

    private Connection spawnConnection() {
        final Connection conn = Connection.connect(config);
        logger.log(System.Logger.Level.DEBUG,
                "connection {0} has been created, free: {1}, used: {2}, max: {3}, pool: {4}",
                conn.getId(),
                connsFree.size(),
                connsUsed.size(),
                config.poolMaxSize(),
                id
        );
        return conn;
    }

    private void addFree(final Connection conn) {
        if (connsFree.offer(conn)) {
            logger.log(System.Logger.Level.DEBUG, "Connection {0} has been added to the free queue, pool: {1}", conn.getId(), id);
        }
        else {
            throw new PGError("Could not add connection %s into the free queue, pool: %s", conn.getId(), id);
        }
    }

    @SuppressWarnings("unused")
    public void returnConnection (final Connection conn) {
        returnConnection(conn, false);
    }

    public void returnConnection (final Connection conn, final boolean forceClose) {
        try (TryLock ignored = lock.get()) {
            returnConnectionLocked(conn, forceClose);
        }
    }
    
    private void returnConnectionLocked(final Connection conn, final boolean forceClose) {

        if (!isUsed(conn)) {
            logger.log(System.Logger.Level.DEBUG, "Connection {0} doesn't belong to the pool {1}, closing", conn.getId(), id);
            closeConnection(conn);
            return;
        }

        removeUsed(conn);

        if (this.isClosed()) {
            closeConnection(conn);
            return;
        }

        if (conn.isClosed()) {
            logger.log(System.Logger.Level.DEBUG, "Connection {0} has already been closed, ignoring. Pool {1}", conn.getId(), id);
            return;
        }

        if (forceClose) {
            logger.log(System.Logger.Level.DEBUG, "Forcibly closing connection {0}, pool: {1}", conn.getId(), id);
            closeConnection(conn);
            return;
        }

        if (conn.isTxError()) {
            logger.log(System.Logger.Level.DEBUG, "connection {0} is in error state, rolling back, pool: {1}", conn.getId(), id);
            conn.rollback();
            closeConnection(conn);
            return;
        }

        if (conn.isTransaction()) {
            logger.log(System.Logger.Level.DEBUG, "connection {0} is in transaction, rolling back, pool: {1}", conn.getId(), id);
            conn.rollback();
        }

        addFree(conn);
    }

    public void close () {
        try (TryLock ignored = lock.get()) {
            closeUnlocked();
        }
    }

    private void closeFree() {
        logger.log(System.Logger.Level.DEBUG, "Closing {0} free connections, pool: {1}", connsFree.size(), id);
        Connection conn;
        while (true) {
            conn = connsFree.poll();
            if (conn == null) {
                break;
            }
            else {
                closeConnection(conn);
            }
        }
    }

    private void closeUsed() {
        logger.log(System.Logger.Level.DEBUG, "Closing {0} used connections, pool: {1}", connsUsed.size(), id);
        Connection conn;
        for (final UUID id: connsUsed.keySet()) {
            conn = connsUsed.get(id);
            Connection.cancelRequest(conn);
            closeConnection(conn);
            connsUsed.remove(id);
        }
    }

    private void closeUnlocked() {
        closeFree();
        closeUsed();
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
                    "<PG pool %s, min: %s, max: %s, expire in: %s>",
                    id,
                    config.poolMinSize(),
                    config.poolMaxSize(),
                    config.poolExpireThresholdMs()
            );
        }
    }

}
