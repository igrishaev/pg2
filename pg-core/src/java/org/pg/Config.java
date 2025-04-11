package org.pg;

import clojure.lang.IFn;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.pg.enums.ConnType;
import org.pg.enums.SSLValidation;
import org.pg.error.PGError;
import org.pg.json.JSON;
import org.pg.processor.IProcessor;

import javax.net.ssl.SSLContext;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.Executor;

public record Config(
        String user,
        String database,
        String password,
        int port,
        String host,
        int protocolVersion,
        Map<String, String> pgParams,
        boolean binaryEncode,
        boolean binaryDecode,
        boolean useSSL,
        boolean SOKeepAlive,
        boolean SOTCPnoDelay,
        int SOTimeout,
        int SOReceiveBufSize,
        int SOSendBufSize,
        int inStreamBufSize,
        int outStreamBufSize,
        IFn fnNotification,
        IFn fnProtocolVersion,
        IFn fnNotice,
        SSLContext sslContext,
        SSLValidation sslValidation,
        long cancelTimeoutMs,
        ObjectMapper objectMapper,
        boolean readOnly,
        int poolMinSize,
        int poolMaxSize,
        int poolExpireThresholdMs,
        int poolBorrowConnTimeoutMs,
        boolean useUnixSocket,
        String unixSocketPath,
        Executor executor,
        boolean readPGTypes
) {

    public ConnType getConnType() {
        if (useUnixSocket || unixSocketPath != null) {
            return ConnType.UNIX_SOCKET;
        } else {
            return ConnType.INET4;
        }
    }

    public static Builder builder (final String user, final String database) {
        return new Builder(user, database);
    }

    @SuppressWarnings("unused")
    public static Config standard (final String user, final String database) {
        return builder(user, database).build();
    }

    public final static class Builder {
        private final String user;
        private final String database;
        private String password = Const.password;
        private int port = Const.PG_PORT;
        private String host = Const.PG_HOST;
        private int protocolVersion = Const.PROTOCOL_VERSION;
        private final Map<String, String> pgParams = new HashMap<>();
        private boolean binaryEncode = Const.BIN_ENCODE;
        private boolean binaryDecode = Const.BIN_DECODE;
        private boolean useSSL = Const.useSSL;
        private boolean SOKeepAlive = Const.SO_KEEP_ALIVE;
        private boolean SOTCPnoDelay = Const.SO_TCP_NO_DELAY;
        private int SOTimeout = Const.SO_TIMEOUT;
        int SOReceiveBufSize = Const.SO_RECV_BUF_SIZE;
        int SOSendBufSize = Const.SO_SEND_BUF_SIZE;
        private int inStreamBufSize = Const.IN_STREAM_BUF_SIZE;
        private int outStreamBufSize = Const.OUT_STREAM_BUF_SIZE;
        private IFn fnNotification;
        private IFn fnProtocolVersion;
        private IFn fnNotice;
        private SSLContext sslContext = null;
        private SSLValidation sslValidation = Const.SSL_VALIDATION;
        private long cancelTimeoutMs = Const.MS_CANCEL_TIMEOUT;
        private ObjectMapper objectMapper = JSON.defaultMapper;
        private boolean readOnly = false;
        private int poolMinSize = Const.POOL_SIZE_MIN;
        private int poolMaxSize = Const.POOL_SIZE_MAX;
        private int poolExpireThresholdMs = Const.POOL_EXPIRE_THRESHOLD_MS;
        private int poolBorrowConnTimeoutMs = Const.POOL_BORROW_CONN_TIMEOUT_MS;
        private boolean useUnixSocket = false;
        private String unixSocketPath = null;
        private Executor executor = Const.executor;
        private boolean readPGTypes = Const.readPGTypes;
        private Map<Object, IProcessor> typeMap;

        public Builder(final String user, final String database) {
            this.user = Objects.requireNonNull(user, "User cannot be null");
            this.database = Objects.requireNonNull(database, "Database cannot be null");
            this.pgParams.put("client_encoding", Const.CLIENT_ENCODING);
            this.pgParams.put("application_name", Const.APP_NAME);
        }

        @SuppressWarnings("unused")
        public Builder sslContext(final SSLContext sslContext) {
            this.useSSL = true;
            this.sslContext = sslContext;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder sslValidation(final SSLValidation sslValidation) {
            this.sslValidation = sslValidation;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder objectMapper(final ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder cancelTimeoutMs(final long cancelTimeoutMs) {
            this.cancelTimeoutMs = cancelTimeoutMs;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder protocolVersion(final int protocolVersion) {
            this.protocolVersion = protocolVersion;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder binaryEncode(final boolean binaryEncode) {
            this.binaryEncode = binaryEncode;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder fnNotification(final IFn fnNotification) {
            this.fnNotification = fnNotification;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder fnNotice(final IFn fnNotice) {
            this.fnNotice = fnNotice;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder fnProtocolVersion(final IFn fnProtocolVersion) {
            this.fnProtocolVersion = Objects.requireNonNull(
                    fnProtocolVersion,
                    "Protocol version function cannot be null"
            );
            return this;
        }

        @SuppressWarnings("unused")
        public Builder binaryDecode(final boolean binaryDecode) {
            this.binaryDecode = binaryDecode;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder useSSL(final boolean useSSL) {
            this.useSSL = useSSL;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder pgParams(final Map<String, String> pgParams) {
            this.pgParams.putAll(pgParams);
            return this;
        }

        @SuppressWarnings("unused")
        public Builder pgParam(final String param, final String value) {
            this.pgParams.put(param, value);
            return this;
        }

        public Builder port(final int port) {
            this.port = port;
            return this;
        }

        public Builder password (final String password) {
            this.password = password;
            return this;
        }

        public Builder host(final String host) {
            this.host = host;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder SOKeepAlive(final boolean SOKeepAlive) {
            this.SOKeepAlive = SOKeepAlive;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder SOTCPnoDelay(final boolean SOTCPnoDelay) {
            this.SOTCPnoDelay = SOTCPnoDelay;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder SOTimeout(final int SOTimeout) {
            this.SOTimeout = SOTimeout;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder SOReceiveBufSize(final int SOReceiveBufSize) {
            this.SOReceiveBufSize = SOReceiveBufSize;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder SOSendBufSize(final int SOSendBufSize) {
            this.SOSendBufSize = SOSendBufSize;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder inStreamBufSize(final int inStreamBufSize) {
            this.inStreamBufSize = inStreamBufSize;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder outStreamBufSize(final int outStreamBufSize) {
            this.outStreamBufSize = outStreamBufSize;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder readOnly() {
            this.readOnly = true;
            return pgParam("default_transaction_read_only", "on");
        }

        @SuppressWarnings("unused")
        public Builder poolMinSize(final int poolMinSize) {
            this.poolMinSize = poolMinSize;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder poolMaxSize(final int poolMaxSize) {
            this.poolMaxSize = poolMaxSize;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder poolExpireThresholdMs(final int poolExpireThresholdMs) {
            this.poolExpireThresholdMs = poolExpireThresholdMs;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder poolBorrowConnTimeoutMs(final int poolBorrowConnTimeoutMs) {
            this.poolBorrowConnTimeoutMs = poolBorrowConnTimeoutMs;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder useUnixSocket(final boolean useUnixSocket) {
            this.useUnixSocket = useUnixSocket;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder unixSocketPath(final String unixSocketPath) {
            this.unixSocketPath = unixSocketPath;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder executor(final Executor executor) {
            this.executor = executor;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder readPGTypes(final boolean readPGTypes) {
            this.readPGTypes = readPGTypes;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder typeMap(final Map<Object, IProcessor> typeMap) {
            this.typeMap = typeMap;
            return this;
        }

        @SuppressWarnings("unused")
        private void _validate() {
            if (!(poolMinSize <= poolMaxSize)) {
                throw new PGError("pool min size (%s) must be <= pool max size (%s)",
                        poolMinSize, poolMaxSize
                );
            }
        }

        public Config build() {
            _validate();
            return new Config(
                    this.user,
                    this.database,
                    this.password,
                    this.port,
                    this.host,
                    this.protocolVersion,
                    Collections.unmodifiableMap(this.pgParams),
                    this.binaryEncode,
                    this.binaryDecode,
                    this.useSSL,
                    this.SOKeepAlive,
                    this.SOTCPnoDelay,
                    this.SOTimeout,
                    this.SOReceiveBufSize,
                    this.SOSendBufSize,
                    this.inStreamBufSize,
                    this.outStreamBufSize,
                    this.fnNotification,
                    this.fnProtocolVersion,
                    this.fnNotice,
                    this.sslContext,
                    this.sslValidation,
                    this.cancelTimeoutMs,
                    this.objectMapper,
                    this.readOnly,
                    this.poolMinSize,
                    this.poolMaxSize,
                    this.poolExpireThresholdMs,
                    this.poolBorrowConnTimeoutMs,
                    this.useUnixSocket,
                    this.unixSocketPath,
                    this.executor,
                    this.readPGTypes
            );
        }
    }
}
