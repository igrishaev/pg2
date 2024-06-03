package org.pg;

import org.pg.error.PGError;
import org.pg.util.TryLock;
import java.util.concurrent.ArrayBlockingQueue;

import java.util.*;
import java.util.concurrent.TimeUnit;

public final class Pool implements AutoCloseable {

    private final UUID id;
    private final Config config;
    private final Map<UUID, Connection> connsUsed;
    private final Map<UUID, Long> connsBorrowedAt;
    private final ArrayBlockingQueue<Connection> connsFree;
    private boolean isClosed = false;
    private final static System.Logger logger = System.getLogger(Pool.class.getCanonicalName());
    private final TryLock lock = new TryLock();
    private final Timer timer;
    
    @Override
    public boolean equals (Object other) {
        return other instanceof Pool && id.equals(((Pool) other).id);
    }

    @Override
    public int hashCode () {
        return this.id.hashCode();
    }

    @SuppressWarnings("unused")
    private void log(final String message) {
        logger.log(config.logLevel(), message);
    }

    private void log(final String message, final Object... args) {
        logger.log(config.logLevel(), String.format(message, args));
    }

    private void logExpired(final Connection conn) {
        log("Connection %s has been expired, closing. Pool: %s",
                conn.getId(),
                this.id
        );
    }

    private void logLeaked(final Connection conn) {
        log("Connection %s has been considered as *leaked*, closing. Pool: %s",
                conn.getId(),
                this.id
        );
    }

    private void logClosed(final Connection conn) {
        log("Connection %s has been closed, closing. Pool: %s",
                conn.getId(),
                this.id
        );
    }

    private void logCreated(final Connection conn) {
        log("connection %s has been created, free: %s, used: %s, max: %s",
                conn.getId(),
                connsFree.size(),
                connsUsed.size(),
                config.poolMaxSize()
        );
    }

    public void replenishConnections() {
        Connection conn;
        log("Start connection replenishment task, pool: %s", id);
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

    private class ReplenishTask extends TimerTask {
        @Override
        public void run() {
            replenishConnections();
        }
    }

    public void closeExpiredConnections() {
        log("Start connection expiration task, pool: %s", id);
        try (TryLock ignored = lock.get()) {
            for (final Connection conn : connsFree) {
                if (isExpired(conn)) {
                    logExpired(conn);
                    closeConnection(conn);
                    removeFree(conn);
                }
            }
        }
    }

    private class ExpireTask extends TimerTask {
        @Override
        public void run() {
            closeExpiredConnections();
        }
    }

    private boolean isLeaked(final Connection conn) {
        final Long borrowedAt = connsBorrowedAt.get(conn.getId());
        if (borrowedAt == null) {
            return false;
        }
        else {
            return System.currentTimeMillis() - borrowedAt > config.poolLeakThresholdMs();
        }
    }

    public void closeLeakedConnections () {
        log("Start leaked connection task, pool: %s", id);
        try (TryLock ignored = lock.get()) {
            for (final Connection conn : connsUsed.values()) {
                if (isLeaked(conn)) {
                    logLeaked(conn);
                    Connection.cancelRequest(conn);
                    closeConnection(conn);
                    removeUsed(conn);
                }
            }
        }
    }

    private class LeakTask extends TimerTask {
        @Override
        public void run() {
            closeLeakedConnections();
        }
    }

    public void checkConnectionsHealth () {
        log("Start SQL check task, pool: %s", id);
        String debug;
        final String sql = config.poolSQLCheck() ;
        try (TryLock ignored = lock.get()) {
            for (final Connection conn : connsFree) {
                debug = String.format("--health check, conn: %s, pool: %s", conn.getId(), id);
                try {
                    conn.query(sql + debug);
                }
                catch (PGError e) {
                    log("Connection %s has failed SQL check, closing. Pool: %s, reason: %s",
                            conn.getId(),
                            id,
                            e.getMessage()
                    );
                    closeConnection(conn);
                    removeFree(conn);
                }
            }
        }
    }

    private class SQLCheckTask extends TimerTask {
        @Override
        public void run() {
            checkConnectionsHealth();
        }
    }

    private Pool (final Config config) {
        final int size = config.poolMaxSize();
        this.id = UUID.randomUUID();
        this.config = config;
        this.connsUsed = new HashMap<>(size);
        this.connsFree = new ArrayBlockingQueue<>(size);
        this.connsBorrowedAt = new HashMap<>(size);
        this.timer = new Timer(String.format("PoolTimer %s", id), false);
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
        return new Pool(config).initiate().setupTimer();
    }

    private Pool initiate() {
        replenishConnections();
        return this;
    }

    private Pool setupTimer() {

        if (true) {
            return this;
        }

        final long periodReplenish = config.poolReplenishPeriodMs();
        if (periodReplenish > 0) {
            final long delayReplenish = (long)(Math.random() * periodReplenish + periodReplenish);
            log("Schedule connection replenishment task, delay: %s, period: %s, pool: %s",
                    delayReplenish, periodReplenish, id);
            timer.schedule(new ReplenishTask(), delayReplenish, periodReplenish);
        }

        final long periodSql = config.poolSQLCheckPeriodMs();
        if (periodSql > 0) {
            final long delaySql = (long)(Math.random() * periodSql + periodSql);
            log("Schedule SQL check task, delay: %s, period: %s, pool: %s",
                    delaySql, periodSql, id);
            timer.schedule(new SQLCheckTask(), delaySql, periodSql);
        }

        final long periodExpire = config.poolExpireThresholdMs();
        if (periodExpire > 0) {
            final long delayExpire = (long)(Math.random() * periodExpire + periodExpire);
            log("Schedule connection expire task, delay: %s, period: %s, pool: %s",
                    delayExpire, periodExpire, id);
            timer.schedule(new ExpireTask(), delayExpire, periodExpire);
        }

        final long periodLeak = config.poolLeakThresholdMs();
        if (periodLeak > 0) {
            final long delayLeak = (long)(Math.random() * periodLeak + periodLeak);
            log("Schedule connection leak task, delay: %s, period: %s, pool: %s",
                    delayLeak, periodLeak, id);
            timer.schedule(new LeakTask(), delayLeak, periodLeak);
        }

        return this;
    }

    private boolean isExpired (final Connection conn) {
        return System.currentTimeMillis() - conn.getCreatedAt() > config.poolExpireThresholdMs();
    }

    private Connection addUsed (final Connection conn) {
        connsUsed.put(conn.getId(), conn);
        connsBorrowedAt.put(conn.getId(), System.currentTimeMillis());
        return conn;
    }

    private void removeUsed (final Connection conn) {
        connsUsed.remove(conn.getId());
        connsBorrowedAt.remove(conn.getId());
    }

    private void removeFree (final Connection conn) {
        connsFree.remove(conn);
    }

    private boolean isUsed (final Connection conn) {
        return connsUsed.containsKey(conn.getId());
    }

    @SuppressWarnings("unused")
    public Connection borrowConnection () {
        try (TryLock ignored = lock.get()) {
            return borrowConnectionUnlocked();
        }
    }

    private Connection preBorrowConnection() {

        if (isClosed()) {
            throw new PGError("Cannot get a connection: the pool has been closed");
        }

        Connection conn;

        // try to get from the queue without waiting
        conn = connsFree.poll();

        // if some, return it
        if (conn != null) {
            return conn;
        }

        // no free connections. If possible, create a new one
        if (connsUsed.size() < config.poolMaxSize()) {
            return spawnConnection();
        }

        // pool max size has been reached. Let's wait hoping that
        // someone will release a connection.
        try {
            conn = connsFree.poll(config.poolPollTimeoutMs(), TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException e) {
            throw new PGError(e, "Interrupted while polling a connection, pool %s", id);
        }

        // if some, return it
        if (conn != null) {
            return conn;
        }

        // no luck. log & throw an error
        final String message = String.format(
                "The pool is exhausted: %s out of %s connections are in use",
                connsUsed.size(),
                config.poolMaxSize()
        );
        log(message);
        throw new PGError(message);

    }

    private Connection borrowConnectionUnlocked() {
        return addUsed(preBorrowConnection());
    }

    private void closeConnection(final Connection conn) {
        conn.close();
        log(
                "the connection %s has been closed, free: %s, used: %s, max: %s",
                conn.getId(),
                connsFree.size(),
                connsUsed.size(),
                config.poolMaxSize()
        );
    }

    private Connection spawnConnection() {
        final Connection conn = Connection.connect(config);
        logCreated(conn);
        return conn;
    }

    @SuppressWarnings("unused")
    public void returnConnection (final Connection conn) {
        returnConnection(conn, false);
    }

    public void returnConnection (final Connection conn, final boolean forceClose) {
        try (TryLock ignored = lock.get()) {
            returnConnectionUnlocked(conn, forceClose);
        }
    }

    private void addFree(final Connection conn) {
        boolean result;
        try {
            result = connsFree.offer(conn, config.poolOfferTimeoutMs(), TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException e) {
            throw new PGError(e, "Interrupted while offering a connection %s to the pool %s",
                    conn.getId(), id);
        }
        if (!result) {
            throw new PGError("Could not return the connection %s to the pool %s", conn.getId(), id);
        }
    }

    private void returnConnectionUnlocked(final Connection conn, final boolean forceClose) {

        if (!isUsed(conn)) {
            log("Connection %s doesn't belong to the pool %s, closing",
                    conn.getId(), id);
            conn.close();
            return;
        }

        removeUsed(conn);

        if (this.isClosed()) {
            closeConnection(conn);
            return;
        }

        if (conn.isClosed()) {
            log("connection %s has already been closed, ignoring. Pool %s",
                    conn.getId(), id);
            return;
        }

        if (forceClose) {
            log("Forcibly closing connection %s, pool: %s", conn.getId(), id);
            closeConnection(conn);
            return;
        }

        if (conn.isTxError()) {
            log("connection %s is in error state, rolling back, pool: %s",
                    conn.getId(), id);
            conn.rollback();
            closeConnection(conn);
            return;
        }

        if (conn.isTransaction()) {
            log("connection %s is in transaction, rolling back, pool: %s",
                    conn.getId(), id);
            conn.rollback();
        }

        addFree(conn);

        log("connection %s has been returned to the pool %s",
                conn.getId(), id);
    }

    public void close () {
        try (TryLock ignored = lock.get()) {
            closeUnlocked();
        }
    }

    private void closeFree() {
        log("Closing %s free connections, pool: %s", connsFree.size(), id);
        Connection conn;
        while (true) {
            conn = connsFree.poll();
            if (conn == null) {
                break;
            }
            else {
                conn.close();
                logClosed(conn);
            }
        }
    }

    private void closeUsed() {
        log("Closing %s used connections, pool: %s", connsUsed.size(), id);
        Connection conn;
        for (final UUID id: connsUsed.keySet()) {
            conn = connsUsed.get(id);
            Connection.cancelRequest(conn);
            conn.close();
            logClosed(conn);
            connsUsed.remove(id);
        }
    }

    private void closeUnlocked() {
        timer.cancel();
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
