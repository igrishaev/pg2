package org.pg;

import clojure.lang.IFn;
import clojure.core$println;

import javax.net.ssl.SSLContext;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.Objects;

public record ConnConfig(
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
        SSLContext sslContext
) {

    public static Builder builder (final String user, final String database) {
        return new Builder(user, database);
    }

    public static ConnConfig standard (final String user, final String database) {
        return builder(user, database).build();
    }

    public static class Builder {
        private final String user;
        private final String database;
        private String password = "";
        private int port = Const.PG_PORT;
        private String host = Const.PG_HOST;
        private int protocolVersion = Const.PROTOCOL_VERSION;
        private final Map<String, String> pgParams = new HashMap<>();
        private boolean binaryEncode = false;
        private boolean binaryDecode = false;
        private boolean useSSL = false;
        private boolean SOKeepAlive = true;
        private boolean SOTCPnoDelay = true;
        private int SOTimeout = Const.SO_TIMEOUT;
        int SOReceiveBufSize = Const.SO_RECV_BUF_SIZE;
        int SOSendBufSize = Const.SO_SEND_BUF_SIZE;
        private int inStreamBufSize = Const.IN_STREAM_BUF_SIZE;
        private int outStreamBufSize = Const.OUT_STREAM_BUF_SIZE;
        private IFn fnNotification = new core$println();
        private IFn fnProtocolVersion = new core$println();
        private IFn fnNotice = new core$println();
        private SSLContext sslContext = null;

        public Builder(final String user, final String database) {
            this.user = Objects.requireNonNull(user);
            this.database = Objects.requireNonNull(database);
            this.pgParams.put("client_encoding", Const.CLIENT_ENCODING);
            this.pgParams.put("application_name", Const.APP_NAME);
        }

        public Builder sslContext(final SSLContext sslContext) {
            this.sslContext = sslContext;
            return this;
        }

        public Builder protocolVersion(final int protocolVersion) {
            this.protocolVersion = protocolVersion;
            return this;
        }

        public Builder binaryEncode(final boolean binaryEncode) {
            this.binaryEncode = binaryEncode;
            return this;
        }

        public Builder fnNotification(final IFn fnNotification) {
            this.fnNotification = Objects.requireNonNull(fnNotification);
            return this;
        }

        public Builder fnNotice(final IFn fnNotice) {
            this.fnNotice = Objects.requireNonNull(fnNotice);
            return this;
        }

        public Builder fnProtocolVersion(final IFn fnProtocolVersion) {
            this.fnProtocolVersion = Objects.requireNonNull(fnProtocolVersion);
            return this;
        }

        public Builder binaryDecode(final boolean binaryDecode) {
            this.binaryDecode = binaryDecode;
            return this;
        }

        public Builder useSSL(final boolean useSSL) {
            this.useSSL = useSSL;
            return this;
        }

        public Builder pgParams(final Map<String, String> pgParams) {
            this.pgParams.putAll(pgParams);
            return this;
        }

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

        public Builder SOKeepAlive(final boolean SOKeepAlive) {
            this.SOKeepAlive = SOKeepAlive;
            return this;
        }

        public Builder SOTCPnoDelay(final boolean SOTCPnoDelay) {
            this.SOTCPnoDelay = SOTCPnoDelay;
            return this;
        }

        public Builder SOTimeout(final int SOTimeout) {
            this.SOTimeout = SOTimeout;
            return this;
        }

        public Builder SOReceiveBufSize(final int SOReceiveBufSize) {
            this.SOReceiveBufSize = SOReceiveBufSize;
            return this;
        }

        public Builder SOSendBufSize(final int SOSendBufSize) {
            this.SOSendBufSize = SOSendBufSize;
            return this;
        }

        public Builder inStreamBufSize(final int inStreamBufSize) {
            this.inStreamBufSize = inStreamBufSize;
            return this;
        }

        public Builder outStreamBufSize(final int outStreamBufSize) {
            this.outStreamBufSize = outStreamBufSize;
            return this;
        }

        public ConnConfig build() {
            return new ConnConfig(
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
                    this.inStreamBufSize,
                    this.outStreamBufSize,
                    this.SOTimeout,
                    this.SOReceiveBufSize,
                    this.SOSendBufSize,
                    this.fnNotification,
                    this.fnProtocolVersion,
                    this.fnNotice,
                    this.sslContext
            );
        }
    }
}
