package org.pg;

import org.pg.error.PGError;
import org.pg.util.TryLock;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

public final class Pool implements AutoCloseable {

    private final UUID id;
    private final Config config;
    private final Map<UUID, Connection> connsUsed;
    private final ArrayBlockingQueue<Connection> connsFree;
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

    public Config getConfig () {
        return config;
    }

    @SuppressWarnings("unused")
    public static Pool clone(final Pool other) {
        return Pool.create(other.getConfig());
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
        this.connsFree = new ArrayBlockingQueue<>(size);
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

    private boolean isUsedLocked(final Connection conn) {
        try (TryLock ignored = lock.get()) {
            return connsUsed.containsKey(conn.getId());
        }
    }

    @SuppressWarnings("unused")
    public Connection borrowConnection () {

        if (isClosed()) {
            throw new PGError("Cannot get a connection: the pool has been closed");
        }

        Connection conn;

        // try to get from the queue without waiting
        while (true) {
            try (TryLock ignored = lock.get()) {
                conn = connsFree.poll();
                if (conn == null) {
                    break;
                } else  {
                    // if expired, close and return null
                    if (isExpired(conn)) {
                        logger.log(System.Logger.Level.DEBUG, "Connection {0} has been expired, closing. Pool: {1}", conn.getId(),  this.id);
                        closeConnection(conn);
                    } else {
                        addUsed(conn);
                        return conn;
                    }
                }
            }
        }

        // no free connections. If possible, create a new one
        try (TryLock ignored = lock.get()) {
            if (connsUsed.size() < config.poolMaxSize()) {
                conn = spawnConnection();
                addUsed(conn);
                return conn;
            }

        }

        try {
            conn = connsFree.poll(config.poolBorrowConnTimeoutMs(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new PGError(e, "Polling was interrupted, pool: %s", id);
        }

        if (conn == null) {
            throw new PGError("Pool %s is exhausted! min: %s, max: %s, free: %s, used: %s, timeout: %s",
                    id,
                    config.poolMinSize(),
                    config.poolMaxSize(),
                    freeCount(),
                    usedCount(),
                    config.poolBorrowConnTimeoutMs()
            );
        }
        else {
            try (TryLock ignored = lock.get()) {
                addUsed(conn);
                return conn;
            }
        }
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
            conn.close();
            logger.log(System.Logger.Level.DEBUG,
                    "Closing conn {0} because there is no room in free connections queue, pool: {1}",
                    conn.getId(), id
            );
        }
    }

    @SuppressWarnings("unused")
    public void returnConnection (final Connection conn) {
        returnConnection(conn, false);
    }

    public void returnConnection (final Connection conn, final boolean forceClose) {

        // doesn't belong to the pool
        if (!isUsedLocked(conn)) {
            logger.log(System.Logger.Level.DEBUG, "Connection {0} doesn't belong to the pool {1}, closing", conn.getId(), id);
            closeConnection(conn);
            return;
        }

        // forcibly close
        if (forceClose) {
            logger.log(System.Logger.Level.DEBUG, "Forcibly closing connection {0}, pool: {1}", conn.getId(), id);
            closeConnection(conn);
            try (TryLock ignored = lock.get()) {
                removeUsed(conn);
            }
            return;
        }

        // transaction has failed
        if (conn.isTxError()) {
            logger.log(System.Logger.Level.DEBUG, "connection {0} is in error state, rolling back, pool: {1}", conn.getId(), id);
            conn.rollback();
            closeConnection(conn);
            try (TryLock ignored = lock.get()) {
                removeUsed(conn);
            }
            return;
        }

        // conn is in transaction
        if (conn.isTransaction()) {
            logger.log(System.Logger.Level.DEBUG, "connection {0} is in transaction, rolling back, pool: {1}", conn.getId(), id);
            conn.rollback();
            try (TryLock ignored = lock.get()) {
                removeUsed(conn);
                addFree(conn);
            }
            return;
        }

        // has been closed by someone else
        if (conn.isClosed()) {
            logger.log(System.Logger.Level.DEBUG, "Connection {0} has already been closed, ignoring. Pool {1}", conn.getId(), id);
            try (TryLock ignored = lock.get()) {
                removeUsed(conn);
            }
            return;
        }

        // pool is closed
        if (this.isClosed()) {
            closeConnection(conn);
            try (TryLock ignored = lock.get()) {
                removeUsed(conn);
            }
            return;
        }

        // else
        try (TryLock ignored = lock.get()) {
            removeUsed(conn);
            addFree(conn);
        }
    }

    public void close () {
        try (TryLock ignored = lock.get()) {
            closeLocked();
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

    private void closeLocked() {
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
