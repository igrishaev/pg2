package org.pg;

import org.pg.error.PGError;
import org.pg.util.TryLock;

import java.util.*;

public final class Pool implements AutoCloseable {

    private final UUID id;
    private final Config config;
    private final Map<UUID, Connection> connsUsed;
    private final Map<UUID, Long> connsBorrowedAt;
    private final Deque<Connection> connsFree;
    private boolean isClosed = false;
    private final static System.Logger logger = System.getLogger(Pool.class.getCanonicalName());
    private final TryLock lock = new TryLock();
    private final Timer timer;

    @SuppressWarnings("unused")
    private void log(final String message) {
        logger.log(config.logLevel(), message);
    }

    private void log(final String message, final Object... args) {
        logger.log(config.logLevel(), String.format(message, args));
    }

    private void logExpired(final Connection conn) {
        logger.log(
                config.logLevel(),
                "Connection {0} has been expired, closing. Pool: {1}",
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

    private class ReplenishTask extends TimerTask {
        @Override
        public void run() {
            log("Start connection replenishment background task, pool: %s", id);
            final int gap = config.poolMinSize() - connsUsed.size();
            try (TryLock ignored = lock.get()) {
                if (gap > 0) {
                    for (var i = 0; i < gap; i++) {
                        spawnConnection();
                    }
                }
            }
        }
    }

    private class ExpireTask extends TimerTask {
        @Override
        public void run() {
            log("Start connection expiration background task, pool: %s", id);
            try (TryLock ignored = lock.get()) {
                for (final Connection conn : connsFree) {
                    if (isExpired(conn)) {
                        logExpired(conn);
                        utilizeConnection(conn);
                        removeUsed(conn);
                    }
                }
            }
        }
    }

    private boolean isLeaked(final Connection conn) {
        return System.currentTimeMillis() - connsBorrowedAt.get(conn.getId()) > config.poolLeakThresholdMs();
    }

    private class LeakTask extends TimerTask {
        @Override
        public void run() {
            try (TryLock ignored = lock.get()) {
                for (final Connection conn : connsUsed.values()) {
                    if (isLeaked(conn)) {
                        logLeaked(conn);
                        Connection.cancelRequest(conn);
                        utilizeConnection(conn);
                        removeUsed(conn);
                    }
                }
            }
        }
    }

    private class SQLCheckTask extends TimerTask {
        @Override
        public void run() {
            final String sql = config.poolSQLCheck();
            if (sql.isEmpty()) {
                return;
            }
            try (TryLock ignored = lock.get()) {
                for (final Connection conn : connsFree) {
                    try {
                        conn.query(sql);
                    }
                    catch (PGError e) {
                        utilizeConnection(conn);
                        removeUsed(conn);
                    }
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
        this.connsBorrowedAt = new HashMap<>(size);
        this.timer = new Timer("PoolTimer", false);
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
        for (int i = 0; i < config.poolMinSize(); i++) {
            spawnConnection();
        }
        return this;
    }

    private Pool setupTimer() {

        final long periodReplenish = config.poolReplenishPeriodMs();
        if (periodReplenish > 0) {
            final long delayReplenish = (long)(Math.random() * periodReplenish + periodReplenish);
            timer.schedule(new ReplenishTask(), delayReplenish, periodReplenish);
        }

        final long periodSql = config.poolSQLCheckPeriodMs();
        if (periodSql > 0) {
            final long delaySql = (long)(Math.random() * periodSql + periodSql);
            timer.schedule(new SQLCheckTask(), delaySql, periodSql);
        }

        final long periodExpire = config.poolExpirePeriodMs();
        if (periodExpire > 0) {
            final long delayExpire = (long)(Math.random() * periodExpire + periodExpire);
            timer.schedule(new ExpireTask(), delayExpire, periodExpire);
        }

        final long periodLeak = config.poolLeakPeriodMs();
        if (periodLeak > 0) {
            final long delayLeak = (long)(Math.random() * periodLeak + periodLeak);
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

    private boolean isUsed (final Connection conn) {
        return connsUsed.containsKey(conn.getId());
    }

    @SuppressWarnings("unused")
    public Connection borrowConnection () {
        try (TryLock ignored = lock.get()) {
            return _borrow_connection_unlocked();
        }
    }

    private Connection _pre_borrow_connection () {

        Connection conn;

        if (isClosed()) {
            throw new PGError("Cannot get a connection: the pool has been closed");
        }

        while (true) {
            conn = connsFree.poll();
            if (conn == null) { // No free connections

                if (connsUsed.size() < config.poolMaxSize()) {
                    return spawnConnection();
                }
                else {
                    final String message = String.format(
                            "The pool is exhausted: %s out of %s connections are in use",
                            connsUsed.size(),
                            config.poolMaxSize()
                    );
                    logger.log(config.logLevel(), message);
                    throw new PGError(message);
                }
            }

            if (conn.isClosed()) {
                final String message = String.format(
                        "Connection %s has been already been closed, ignoring",
                        conn.getId()
                );
                logger.log(config.logLevel(), message);
            }
            else {
                return conn;
            }
        }
    }

    private Connection _borrow_connection_unlocked () {
        return addUsed(_pre_borrow_connection());
    }

    private void utilizeConnection(final Connection conn) {
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
        offerConnection(conn);
        logCreated(conn);
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
            logger.log(
                    config.logLevel(),
                    "connection {0} doesn't belong to the pool {1}, ignoring",
                    conn.getId(), id
            );
            return;
        }

        removeUsed(conn);

        if (this.isClosed()) {
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
            closeUnlocked();
        }
    }

    private void closeFree() {
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
                    "<PG pool %s, min: %s, max: %s, lifetime: %s>",
                    id,
                    config.poolMinSize(),
                    config.poolMaxSize(),
                    config.poolExpireThresholdMs()
            );
        }
    }

}
