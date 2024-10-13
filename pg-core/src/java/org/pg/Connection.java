package org.pg;

import clojure.lang.*;
import org.pg.auth.MD5;
import org.pg.auth.ScramSha256;
import org.pg.clojure.KW;
import org.pg.clojure.RowMap;
import org.pg.codec.CodecParams;
import org.pg.copy.Copy;
import org.pg.enums.*;
import org.pg.error.PGError;
import org.pg.msg.*;
import org.pg.msg.client.*;
import org.pg.msg.server.*;
import org.pg.type.processor.IProcessor;
import org.pg.util.*;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneId;
import java.util.*;
import java.net.Socket;
import java.nio.ByteBuffer;

public final class Connection implements AutoCloseable {

    private final Config config;
    private final UUID id;
    private final long createdAt;
    private int counter = 0;

    private int pid;
    private int secretKey;
    private TXStatus txStatus;
    private Socket socket;
    private InputStream inStream;
    private OutputStream outStream;
    private final Map<String, String> params;
    private CodecParams codecParams;
    private boolean isSSL = false;
    private final TryLock lock = new TryLock();
    private boolean isClosed = false;

    @Override
    public boolean equals (Object other) {
        return other instanceof Connection && id.equals(((Connection) other).id);
    }

    @Override
    public int hashCode () {
        return this.id.hashCode();
    }

    private Connection(final Config config) {
        this.config = config;
        this.params = new HashMap<>();
        this.codecParams = CodecParams.builder().objectMapper(config.objectMapper()).build();
        this.id = UUID.randomUUID();
        this.createdAt = System.currentTimeMillis();
    }

    public static Connection connect(final Config config, final boolean sendStartup) {
        final Connection conn = new Connection(config);
        conn._connect();
        conn._setSocketOptions();
        conn._preSSLStage();
        if (sendStartup) {
            conn._authenticate();
            conn._initTypeMapping();
        }
        return conn;
    }

    @SuppressWarnings("unused")
    public static Connection connect(final Config config) {
        return connect(config, true);
    }

    @SuppressWarnings("unused")
    public static Connection connect(final String host,
                                     final int port,
                                     final String user,
                                     final String password,
                                     final String database)
    {
        return new Connection(Config.builder(user, database)
                .host(host)
                .port(port)
                .password(password)
                .build());
    }

    public void close () {
        try (TryLock ignored = lock.get()) {
            if (!isClosed) {
                sendTerminate();
                flush();
                IOTool.close(socket);
                isClosed = true;
            }
        }

    }

    private void _initTypeMapping() {
        final Map<String, IProcessor> sourceMap = config.typeMap();
        final int len = sourceMap.size();
        if (len == 0) {
            return;
        }
        int i = 0;
        String[] qMarks = new String[len];
        String[] sqlParams = new String[len];
        for (final Map.Entry<String, IProcessor> entry: sourceMap.entrySet()) {
            String usertype = entry.getKey();
            sqlParams[i] = usertype;
            qMarks[i] = "$" + (i + 1);
            i++;
        }

        final ExecuteParams executeParams = ExecuteParams.builder()
                .params(sqlParams)
                .build();

        APersistentVector result = (APersistentVector) execute(
                "select pg_type.oid, pg_namespace.nspname || '.' || pg_type.typname as type " +
                        "from pg_type, pg_namespace " +
                        "where " +
                        "pg_type.typnamespace = pg_namespace.oid " +
                        "and pg_namespace.nspname || '.' || pg_type.typname in (" +
                        String.join(", ", qMarks) +
                        ");",
                executeParams
        );

        int oid;
        String type;

        for (final Object x: result) {
            RowMap rm = (RowMap) x;
            oid = (int) rm.get(KW.oid);
            type = (String) rm.get(KW.type);
            IProcessor iProcessor = sourceMap.get(type);
            if (iProcessor != null) {
                codecParams.setProcessor(oid, iProcessor);
            }
        }

    }

    private void _setSocketOptions () {
        try {
            socket.setTcpNoDelay(config.SOTCPnoDelay());
            socket.setSoTimeout(config.SOTimeout());
            socket.setKeepAlive(config.SOKeepAlive());
            socket.setReceiveBufferSize(config.SOReceiveBufSize());
            socket.setSendBufferSize(config.SOSendBufSize());
        }
        catch (IOException e) {
            throw new PGError(e, "couldn't set socket options");
        }
    }

    private int nextInt () {
        try (TryLock ignored = lock.get()) {
            return ++counter;
        }
    }

    @SuppressWarnings("unused")
    public int getPid () {
        try (TryLock ignored = lock.get()) {
            return pid;
        }
    }

    public UUID getId() {
        return id;
    }

    @SuppressWarnings("unused")
    public long getCreatedAt () {
        return createdAt;
    }

    public Boolean isClosed () {
        try (TryLock ignored = lock.get()) {
            return isClosed;
        }
    }

    @SuppressWarnings("unused")
    public TXStatus getTxStatus () {
        try (TryLock ignored = lock.get()) {
            return txStatus;
        }
    }

    @SuppressWarnings("unused")
    public boolean isSSL () {
        try (TryLock ignored = lock.get()) {
            return isSSL;
        }
    }

    @SuppressWarnings("unused")
    public String getParam (final String param) {
        try (TryLock ignored = lock.get()) {
            return params.get(param);
        }
    }

    @SuppressWarnings("unused")
    public IPersistentMap getParams () {
        try (TryLock ignored = lock.get()) {
            return PersistentHashMap.create(params);
        }
    }

    private void setParam (final String param, final String value) {
        params.put(param, value);
        switch (param) {
            case "client_encoding" -> {
                final Charset clientCharset = Charset.forName(value);
                codecParams = codecParams.withClientCharset(clientCharset);
            }
            case "server_encoding" -> {
                final Charset serverCharset = Charset.forName(value);
                codecParams = codecParams.withServerCharset(serverCharset);
            }
            case "DateStyle" -> codecParams = codecParams.withDateStyle(value);
            case "TimeZone" -> {
                final ZoneId timeZone = ZoneId.of(value);
                codecParams = codecParams.withTimeZoneId(timeZone);
            }
            case "integer_datetimes" -> {
                final boolean integerDatetime = value.equals("on");
                codecParams = codecParams.withIntegerDatetime(integerDatetime);
            }
        }
    }

    public Config getConfig () {
        return config;
    }

    public Integer getPort () {
        return config.port();
    }

    public String getHost () {
        return config.host();
    }

    public String getUser () {
        return config.user();
    }

    @SuppressWarnings("unused")
    private Map<String, String> getPgParams () {
        return config.pgParams();
    }

    public String getDatabase () {
        return config.database();
    }

    public String toString () {
        return String.format("<PG connection %s@%s:%s/%s>",
                             getUser(),
                             getHost(),
                             getPort(),
                             getDatabase());
    }

    private void _authenticate () {
        sendStartupMessage();
        interact(true);
    }

    private boolean readSSLResponse () {
        final char c = (char) IOTool.read(inStream);
        return switch (c) {
            case 'N' -> false;
            case 'S' -> true;
            default -> throw new PGError("wrong SSL response: %s", c);
        };
    }

    private static final String[] SSLProtocols = new String[] {
            "TLSv1.2",
            "TLSv1.1",
            "TLSv1"
    };

    private SSLContext getSSLContext () throws NoSuchAlgorithmException {
        final SSLContext configContext = config.sslContext();
        if (configContext == null) {
            return SSLContext.getDefault();
        }
        else {
            return configContext;
        }
    }

    private void upgradeToSSL () throws NoSuchAlgorithmException, IOException {
        final SSLContext sslContext = getSSLContext();
        final SSLSocket sslSocket = (SSLSocket) sslContext.getSocketFactory().createSocket(
                socket,
                config.host(),
                config.port(),
                true
        );

        final InputStream sslInStream = new BufferedInputStream(
                IOTool.getInputStream(sslSocket),
                config.inStreamBufSize()
        );

        final OutputStream sslOutStream = new BufferedOutputStream(
                IOTool.getOutputStream(sslSocket),
                config.outStreamBufSize()
        );

        sslSocket.setUseClientMode(true);
        sslSocket.setEnabledProtocols(SSLProtocols);
        sslSocket.startHandshake();

        socket = sslSocket;
        inStream = sslInStream;
        outStream = sslOutStream;
        isSSL = true;
    }

    private void _preSSLStage () {
        if (config.useSSL()) {
            final SSLRequest msg = new SSLRequest(Const.SSL_CODE);
            sendMessage(msg);
            flush();
            final boolean ssl = readSSLResponse();
            if (ssl) {
                try {
                    upgradeToSSL();
                }
                catch (Throwable e) {
                    close();
                    throw new PGError(
                            e,
                            "could not upgrade to SSL due to an exception: %s",
                            e.getMessage()
                    );
                }
            }
            else {
                close();
                throw new PGError("the server is configured to not use SSL");
            }
        }
    }

    private void _connect () {
        try (TryLock ignored = lock.get()) {
            _connect_unlocked();
        }
    }

    private void _connect_unlocked () {
        final int port = getPort();
        final String host = getHost();
        socket = IOTool.socket(host, port);
        inStream = new BufferedInputStream(
                IOTool.getInputStream(socket),
                config.inStreamBufSize()
        );
        outStream = new BufferedOutputStream(
                IOTool.getOutputStream(socket),
                config.outStreamBufSize()
        );
    }

    // Send bytes into the output stream. Do not flush the buffer,
    // must be done manually.
    private void sendBytes (final byte[] buf) {
        if (Debug.isON) {
            Debug.debug(" <- sendBytes: %s", Arrays.toString(buf));
        }
        IOTool.write(outStream, buf);
    }

    // Like sendBytes above but taking boundaries into account.
    private void sendBytes (final byte[] buf, final int offset, final int len) {
        IOTool.write(outStream, buf, offset, len);
    }

    private void sendBytesCopy(final byte[] bytes) {
        final ByteBuffer bb = ByteBuffer.allocate(5);
        bb.put((byte)'d');
        bb.putInt(4 + bytes.length);
        sendBytes(bb.array());
        sendBytes(bytes);
    }

    private void sendMessage (final IClientMessage msg) {
        if (Debug.isON) {
            Debug.debug(" <- %s", msg);
        }
        final ByteBuffer buf = msg.encode(codecParams.clientCharset());
        IOTool.write(outStream, buf.array());
    }

    private String generateStatement () {
        return String.format("s%d", nextInt());
    }

    private String generatePortal () {
        return String.format("p%d", nextInt());
    }

    private void sendStartupMessage () {
        final StartupMessage msg =
            new StartupMessage(
                    config.protocolVersion(),
                    config.user(),
                    config.database(),
                    config.pgParams()
            );
        sendMessage(msg);
    }

    private void sendCopyData (final byte[] buf) {
        sendMessage(new CopyData(ByteBuffer.wrap(buf)));
    }

    private void sendCopyDone () {
        sendMessage(CopyDone.INSTANCE);
    }

    private void sendCopyFail (final String errorMessage) {
        sendMessage(new CopyFail(errorMessage));
    }

    private void sendQuery (final String query) {
        sendMessage(new Query(query));
    }

    private void sendPassword (final String password) {
        sendMessage(new PasswordMessage(password));
    }

    private void sendSync () {
        sendMessage(Sync.INSTANCE);
    }

    private void sendFlush () {
        sendMessage(Flush.INSTANCE);
    }

    private void sendTerminate () {
        sendMessage(Terminate.INSTANCE);
    }

    @SuppressWarnings("unused")
    private void sendSSLRequest () {
        sendMessage(new SSLRequest(Const.SSL_CODE));
    }

    private IServerMessage readMessage (final boolean skipMode) {

        final byte[] bufHeader = IOTool.readNBytes(inStream, 5);
        final ByteBuffer bbHeader = ByteBuffer.wrap(bufHeader);

        final char tag = (char) bbHeader.get();
        final int bodySize = bbHeader.getInt() - 4;

        // skipMode means there has been an exception before. There is no need
        // to parse data-heavy messages as we're going to throw an exception
        // at the end anyway. If there is a DataRow or a CopyData message,
        // just skip it.
        if (skipMode) {
            if (tag == 'D' || tag == 'd') {
                IOTool.skip(inStream, bodySize);
                return SkippedMessage.INSTANCE;
            }
        }

        byte[] bufBody = IOTool.readNBytes(inStream, bodySize);
        ByteBuffer bbBody = ByteBuffer.wrap(bufBody);

        return switch (tag) {
            case 'R' -> AuthenticationResponse.fromByteBuffer(bbBody, codecParams.serverCharset());
            case 'S' -> ParameterStatus.fromByteBuffer(bbBody, codecParams.serverCharset());
            case 'Z' -> ReadyForQuery.fromByteBuffer(bbBody);
            case 'C' -> CommandComplete.fromByteBuffer(bbBody, codecParams.serverCharset());
            case 'T' -> RowDescription.fromByteBuffer(bbBody, codecParams.serverCharset());
            case 'D' -> DataRow.fromByteBuffer(bbBody);
            case 'E' -> ErrorResponse.fromByteBuffer(bbBody, codecParams.serverCharset());
            case 'K' -> BackendKeyData.fromByteBuffer(bbBody);
            case '1' -> ParseComplete.INSTANCE;
            case '2' -> BindComplete.INSTANCE;
            case '3' -> CloseComplete.INSTANCE;
            case 't' -> ParameterDescription.fromByteBuffer(bbBody);
            case 'H' -> CopyOutResponse.fromByteBuffer(bbBody);
            case 'd' -> CopyData.fromByteBuffer(bbBody);
            case 'c' -> CopyDone.INSTANCE;
            case 'I' -> EmptyQueryResponse.INSTANCE;
            case 'n' -> NoData.INSTANCE;
            case 'v' -> NegotiateProtocolVersion.fromByteBuffer(bbBody, codecParams.serverCharset());
            case 'A' -> NotificationResponse.fromByteBuffer(bbBody, codecParams.serverCharset());
            case 'N' -> NoticeResponse.fromByteBuffer(bbBody, codecParams.serverCharset());
            case 's' -> PortalSuspended.INSTANCE;
            case 'G' -> CopyInResponse.fromByteBuffer(bbBody);
            default -> throw new PGError("Unknown message: %s", tag);
        };

    }

    private void sendDescribeStatement (final String statement) {
        final Describe msg = new Describe(SourceType.STATEMENT, statement);
        sendMessage(msg);
    }

    private void sendDescribePortal (final String portal) {
        final Describe msg = new Describe(SourceType.PORTAL, portal);
        sendMessage(msg);
    }

    private void sendExecute (final String portal, final long maxRows) {
        final Execute msg = new Execute(portal, maxRows);
        sendMessage(msg);
    }

    public Object query(final String sql) {
        return query(sql, ExecuteParams.INSTANCE);
    }

    public Object query(final String sql, final ExecuteParams executeParams) {
        try (TryLock ignored = lock.get()) {
            sendQuery(sql);
            return interact(executeParams).getResult();
        }
    }

    @SuppressWarnings("unused")
    public PreparedStatement prepare (final String sql) {
        return prepare(sql, ExecuteParams.INSTANCE);
    }

    public PreparedStatement prepare (final String sql, final ExecuteParams executeParams) {
        try (TryLock ignored = lock.get()) {
            return _prepare_unlocked(sql, executeParams);
        }
    }

    private PreparedStatement _prepare_unlocked (
            final String sql,
            final ExecuteParams executeParams
    ) {
        final String statement = generateStatement();
        final int[] OIDs = executeParams.OIDs();
        final Parse parse = new Parse(statement, sql, OIDs);
        sendMessage(parse);
        sendDescribeStatement(statement);
        sendSync();
        sendFlush();
        final Result res = interact();
        final ParameterDescription paramDesc = res.getParameterDescription();
        final RowDescription rowDescription = res.getRowDescription();
        return new PreparedStatement(parse, paramDesc, rowDescription);
    }

    private void sendBind (final String portal,
                           final PreparedStatement stmt,
                           final ExecuteParams executeParams
    ) {
        final List<Object> params = executeParams.params();
        final int[] OIDs = stmt.parameterDescription().OIDs();
        final int size = params.size();

        if (size != OIDs.length) {
            throw new PGError(
                    "Wrong parameters count: %s (must be %s)",
                    size, OIDs.length
            );
        }

        final Format paramsFormat = (executeParams.binaryEncode() || config.binaryEncode()) ? Format.BIN : Format.TXT;
        final Format columnFormat = (executeParams.binaryDecode() || config.binaryDecode()) ? Format.BIN : Format.TXT;

        final byte[][] bytes = new byte[size][];
        String statement = stmt.parse().statement();

        IProcessor typeProcessor;

        int i = -1;
        for (final Object param: params) {
            i++;
            if (param == null) {
                bytes[i] = null;
                continue;
            }
            int oid = OIDs[i];
            typeProcessor = codecParams.getProcessor(oid);

            switch (paramsFormat) {
                case BIN -> {
                    ByteBuffer buf = typeProcessor.encodeBin(param, codecParams);
                    bytes[i] = buf.array();
                }
                case TXT -> {
                    String value = typeProcessor.encodeTxt(param, codecParams);
                    bytes[i] = value.getBytes(codecParams.clientCharset());
                }
                default ->
                    throw new PGError("unknown format: %s", paramsFormat);
            }
        }
        final Bind msg = new Bind(
                portal,
                statement,
                bytes,
                paramsFormat,
                columnFormat
        );

        for (byte[] buf: msg.toByteArrays()) {
            sendBytes(buf);
        }
    }

    private void flush () {
        IOTool.flush(outStream);
    }

    @SuppressWarnings("unused")
    public Object executeStatement(final PreparedStatement stmt) {
        return executeStatement(stmt, ExecuteParams.INSTANCE);
    }

    public Object executeStatement (
            final PreparedStatement stmt,
            final ExecuteParams executeParams
    ) {
        try (TryLock ignored = lock.get()) {
            final String portal = generatePortal();
            sendBind(portal, stmt, executeParams);
            sendDescribePortal(portal);
            sendExecute(portal, executeParams.maxRows());
            sendClosePortal(portal);
            sendSync();
            sendFlush();
            return interact(executeParams).getResult();
        }
    }

    @SuppressWarnings("unused")
    public Object execute (final String sql) {
        return execute(sql, ExecuteParams.INSTANCE);
    }

    public Object execute (final String sql, final List<Object> params) {
        return execute(sql, ExecuteParams.builder().params(params).build());
    }

    public Object execute (final String sql, final ExecuteParams executeParams) {
        try (final TryLock ignored = lock.get()) {
            final PreparedStatement stmt = prepare(sql, executeParams);
            final String portal = generatePortal();
            sendBind(portal, stmt, executeParams);
            sendDescribePortal(portal);
            sendExecute(portal, executeParams.maxRows());
            sendClosePortal(portal);
            sendCloseStatement(stmt);
            sendSync();
            sendFlush();
            return interact(executeParams).getResult();
        }
    }

    private void sendCloseStatement (final PreparedStatement stmt) {
        final Close msg = new Close(SourceType.STATEMENT, stmt.parse().statement());
        sendMessage(msg);
    }

    private void sendCloseStatement (final String statement) {
        final Close msg = new Close(SourceType.STATEMENT, statement);
        sendMessage(msg);
    }

    private void sendClosePortal (final String portal) {
        final Close msg = new Close(SourceType.PORTAL, portal);
        sendMessage(msg);
    }

    @SuppressWarnings("unused")
    public void closeStatement (final PreparedStatement statement) {
        closeStatement(statement.parse().statement());
    }

    public void closeStatement (final String statement) {
        try (TryLock ignored = lock.get()) {
            sendCloseStatement(statement);
            sendSync();
            sendFlush();
            interact();
        }
    }

    private Result interact (final ExecuteParams executeParams) {
        return interact(executeParams, false);
    }

    private Result interact (final ExecuteParams executeParams, final boolean isAuth) {
        flush();
        final Result res = new Result(executeParams);
        while (true) {
            final IServerMessage msg = readMessage(res.hasException());
            if (Debug.isON) {
                Debug.debug(" -> %s", msg);
            }
            handleMessage(msg, res);
            if (isEnough(msg, isAuth)) {
                break;
            }
        }
        res.maybeThrowError();
        return res;
    }

    private Result interact (final boolean isAuth) {
        return interact(ExecuteParams.INSTANCE, isAuth);
    }

    private Result interact () {
        return interact(ExecuteParams.INSTANCE, false);
    }

    private static void noop () {}

    private void handleMessage (final IServerMessage msg, final Result res) {

        if (msg instanceof DataRow x) {
            handleDataRow(x, res);
        } else if (msg instanceof NotificationResponse x) {
            handleNotificationResponse(x);
        } else if (msg instanceof AuthenticationCleartextPassword) {
            handleAuthenticationCleartextPassword();
        } else if (msg instanceof AuthenticationSASL x) {
            handleAuthenticationSASL(x, res);
        } else if (msg instanceof AuthenticationSASLContinue x) {
            handleAuthenticationSASLContinue(x, res);
        } else if (msg instanceof AuthenticationSASLFinal x) {
            handleAuthenticationSASLFinal(x, res);
        } else if (msg instanceof NoticeResponse x) {
            handleNoticeResponse(x);
        } else if (msg instanceof ParameterStatus x) {
            handleParameterStatus(x);
        } else if (msg instanceof RowDescription x) {
            handleRowDescription(x, res);
        } else if (msg instanceof ReadyForQuery x) {
            handleReadyForQuery(x);
        } else if (msg instanceof PortalSuspended x) {
            handlePortalSuspended(x, res);
        } else if (msg instanceof AuthenticationMD5Password x) {
            handleAuthenticationMD5Password(x);
        } else if (msg instanceof NegotiateProtocolVersion x) {
            handleNegotiateProtocolVersion(x);
        } else if (msg instanceof CommandComplete x) {
            handleCommandComplete(x, res);
        } else if (msg instanceof ErrorResponse x) {
            handleErrorResponse(x, res);
        } else if (msg instanceof BackendKeyData x) {
            handleBackendKeyData(x);
        } else if (msg instanceof ParameterDescription x) {
            handleParameterDescription(x, res);
        } else if (msg instanceof ParseComplete) {
            noop();
        } else if (msg instanceof CopyOutResponse) {
            noop();
        } else if (msg instanceof CopyData x) {
            handleCopyData(x, res);
        } else if (msg instanceof CopyInResponse) {
            handleCopyInResponse(res);
        } else if (msg instanceof NoData) {
            noop();
        } else if (msg instanceof EmptyQueryResponse) {
            noop();
        } else if (msg instanceof CloseComplete) {
            noop();
        } else if (msg instanceof BindComplete) {
            noop();
        } else if (msg instanceof AuthenticationOk) {
            noop();
        } else if (msg instanceof CopyDone) {
            noop();
        } else if (msg instanceof SkippedMessage) {
            noop();
        } else {
            throw new PGError("Cannot handle this message: %s", msg);
        }

    }

    private void handleAuthenticationSASL (final AuthenticationSASL msg, final Result res) {

        res.scramPipeline = ScramSha256.pipeline();

        if (msg.isScramSha256()) {
            final ScramSha256.Step1 step1 = ScramSha256.step1_clientFirstMessage(
                    config.user(), config.password()
            );
            final SASLInitialResponse msgSASL = new SASLInitialResponse(
                    SASL.SCRAM_SHA_256,
                    step1.clientFirstMessage()
            );
            res.scramPipeline.step1 = step1;
            sendMessage(msgSASL);
            flush();
        }

        if (msg.isScramSha256Plus()) {
            throw new PGError("SASL SCRAM SHA 256 PLUS method is not implemented yet");
        }
    }

    private void handleAuthenticationSASLContinue (final AuthenticationSASLContinue msg, final Result res) {
        final ScramSha256.Step1 step1 = res.scramPipeline.step1;
        final String serverFirstMessage = msg.serverFirstMessage();
        final ScramSha256.Step2 step2 = ScramSha256.step2_serverFirstMessage(serverFirstMessage);
        final ScramSha256.Step3 step3 = ScramSha256.step3_clientFinalMessage(step1, step2);
        res.scramPipeline.step2 = step2;
        res.scramPipeline.step3 = step3;
        final SASLResponse msgSASL = new SASLResponse(step3.clientFinalMessage());
        sendMessage(msgSASL);
        flush();
    }

    private void handleAuthenticationSASLFinal (final AuthenticationSASLFinal msg, final Result res) {
        final String serverFinalMessage = msg.serverFinalMessage();
        final ScramSha256.Step4 step4 = ScramSha256.step4_serverFinalMessage(serverFinalMessage);
        res.scramPipeline.step4 = step4;
        final ScramSha256.Step3 step3 = res.scramPipeline.step3;
        ScramSha256.step5_verifyServerSignature(step3, step4);
    }

    private void handleCopyInResponseStream (Result res) {

        final int bufSize = res.executeParams.copyBufSize();
        final byte[] buf = new byte[bufSize];

        final ByteBuffer bbLead = ByteBuffer.allocate(5);
        bbLead.put((byte)'d');

        InputStream inputStream = res.executeParams.inputStream();

        Throwable e = null;
        int read;

        while (true) {
            try {
                read = inputStream.read(buf);
            }
            catch (Throwable caught) {
                e = caught;
                break;
            }

            if (read == -1) {
                break;
            }

            bbLead.position(1);
            bbLead.putInt(4 + read);

            sendBytes(bbLead.array());
            sendBytes(buf, 0, read);
        }

        if (e == null) {
            sendCopyDone();
        }
        else {
            res.setException(e);
            sendCopyFail(Const.COPY_FAIL_EXCEPTION_MSG);
        }
    }

    private void handleCopyInResponseData (final Result res, final Iterator<List<Object>> rows) {
        final ExecuteParams executeParams = res.executeParams;
        final CopyFormat format = executeParams.copyFormat();
        Throwable e = null;

        switch (format) {

            case CSV:
                String line;
                while (rows.hasNext()) {
                    try {
                        line = Copy.encodeRowCSV(rows.next(), executeParams, codecParams);
                    }
                    catch (Throwable caught) {
                        e = caught;
                        break;
                    }
                    final byte[] bytes = line.getBytes(StandardCharsets.UTF_8);
                    sendBytesCopy(bytes);
                }
                break;

            case BIN:
                ByteBuffer buf;
                // TODO: use sendBytes
                sendCopyData(Copy.COPY_BIN_HEADER);
                while (rows.hasNext()) {
                    try {
                        buf = Copy.encodeRowBin(rows.next(), executeParams, codecParams);
                    }
                    catch (Throwable caught) {
                        e = caught;
                        break;
                    }
                    sendBytesCopy(buf.array());
                }
                if (e == null) {
                    sendBytes(Copy.MSG_COPY_BIN_TERM);
                }
                break;

            case TAB:
                e = new PGError("TAB COPY format is not implemented");
                break;
        }

        if (e == null) {
            sendCopyDone();
        }
        else {
            res.setException(e);
            sendCopyFail(Const.COPY_FAIL_EXCEPTION_MSG);
        }
    }

    private void handleCopyInResponseRows (final Result res) {
        final Iterator<List<Object>> iterator = res.executeParams.copyInRows()
                .stream()
                .filter(Objects::nonNull)
                .iterator();
        handleCopyInResponseData(res, iterator);
    }

    private void handleCopyInResponseMaps (final Result res) {
        final List<Object> keys = res.executeParams.copyInKeys();
        final Iterator<List<Object>> iterator = res.executeParams.copyInMaps()
                .stream()
                .filter(Objects::nonNull)
                .map(map -> mapToRow(map, keys))
                .iterator();
        handleCopyInResponseData(res, iterator);
    }

    private void handleCopyInResponse (Result res) {

        // These three methods only send data but do not read.
        // Thus, we rely on sendBytes which doesn't trigger flushing
        // the output stream. Flushing is expensive and thus must be called
        // manually when all the data has been sent.
        if (res.executeParams.isCopyInRows()) {
            handleCopyInResponseRows(res);
        }
        else if (res.executeParams.isCopyInMaps()) {
            handleCopyInResponseMaps(res);
        } else {
            handleCopyInResponseStream(res);
        }
        // Finally, we flush the output stream so all unsent bytes get sent.
        flush();
    }

    private void handlePortalSuspended (final PortalSuspended msg, final Result res) {
        res.handlePortalSuspended(msg);
    }

    private void handlerCall (final IFn f, final Object arg) {
        if (f != null) {
            Agent.soloExecutor.submit(() -> {
                f.invoke(arg);
            });
        }
    }

    private void handleNotificationResponse (final NotificationResponse msg) {
        handlerCall(config.fnNotification(), msg.toClojure());
    }

    private void handleNoticeResponse (final NoticeResponse msg) {
        handlerCall(config.fnNotice(), msg.toClojure());
    }

    private void handleNegotiateProtocolVersion (final NegotiateProtocolVersion msg) {
        handlerCall(config.fnProtocolVersion(), msg.toClojure());
    }

    private void handleAuthenticationMD5Password (final AuthenticationMD5Password msg) {
        final String hashed = MD5.hashPassword(
                config.user(),
                config.password(),
                msg.salt()
        );
        sendPassword(hashed);
        flush();
    }

    private void handleCopyData (final CopyData msg, final Result res) {
        try {
            handleCopyDataUnsafe(msg, res);
        } catch (Throwable e) {
            res.setException(e);
        }
    }

    private void handleCopyDataUnsafe (final CopyData msg, final Result res) throws IOException {
        final OutputStream outputStream = res.executeParams.outputStream();
        final byte[] bytes = msg.buf().array();
        outputStream.write(bytes);
    }

    @SuppressWarnings("unused")
    public AutoCloseable getLock() {
        return lock.get();
    }

    @SuppressWarnings("unused")
    public Object copy (final String sql, final ExecuteParams executeParams) {
        try (TryLock ignored = lock.get()) {
            sendQuery(sql);
            final Result res = interact(executeParams);
            return res.getResult();
        }
    }

    private static List<Object> mapToRow (final Map<?,?> map, final List<Object> keys) {
        final List<Object> row = new ArrayList<>(keys.size());
        for (final Object key: keys) {
            row.add(map.get(key));
        }
        return row;
    }

    private void handleParameterDescription (final ParameterDescription msg, final Result res) {
        res.handleParameterDescription(msg);
    }

    private void handleAuthenticationCleartextPassword () {
        sendPassword(config.password());
        flush();
    }

    private void handleParameterStatus (final ParameterStatus msg) {
        setParam(msg.param(), msg.value());
    }

    private static void handleRowDescription (final RowDescription msg, final Result res) {
        res.handleRowDescription(msg);
    }

    private void handleDataRowUnsafe (final DataRow msg, final Result res) {
        final RowDescription rowDescription = res.getRowDescription();
        final Map<Object, Short> keysIndex = res.getCurrentKeysIndex();
        final Object[] keys = res.getCurrentKeys();
        final RowMap rowMap = new RowMap(
                lock,
                msg,
                rowDescription,
                keys,
                keysIndex,
                codecParams
        );
        res.addClojureRow(rowMap);
    }

    private void handleDataRow (final DataRow msg, final Result res) {
        try {
            handleDataRowUnsafe(msg, res);
        }
        catch (Throwable e) {
            res.setException(e);
        }
    }

    private void handleReadyForQuery (final ReadyForQuery msg) {
        txStatus = msg.txStatus();
    }

    private static void handleCommandComplete (final CommandComplete msg, final Result res) {
        res.handleCommandComplete(msg);
    }

    private static void handleErrorResponse (final ErrorResponse msg, final Result res) {
        res.addErrorResponse(msg);
    }

    private void handleBackendKeyData (final BackendKeyData msg) {
        pid = msg.pid();
        secretKey = msg.secretKey();
    }

    private static Boolean isEnough (final IServerMessage msg, final boolean isAuth) {
        return switch (msg.getClass().getSimpleName()) {
            case "ReadyForQuery" -> true;
            case "ErrorResponse" -> isAuth;
            default -> false;
        };
    }

    @SuppressWarnings("unused")
    public static Connection clone (final Connection conn) {
        return Connection.connect(conn.config);
    }

    @SuppressWarnings("unused")
    public static void cancelRequest(final Connection conn) {
        final CancelRequest msg = new CancelRequest(Const.CANCEL_CODE, conn.pid, conn.secretKey);
        final Connection temp = Connection.connect(conn.config, false);
        temp.sendMessage(msg);
        temp.close();
    }

    @SuppressWarnings("unused")
    public void begin () {
        begin(TxLevel.NONE, false);
    }

    @SuppressWarnings("unused")
    public void begin (final TxLevel txLevel) {
        begin(txLevel, false);
    }

    @SuppressWarnings("unused")
    public void begin (final TxLevel txLevel, final boolean readOnly) {
        String query = "BEGIN TRANSACTION";
        if (txLevel != TxLevel.NONE) {
            query += " ISOLATION LEVEL " + txLevel.getCode();
        }

        final boolean readOnlyFinal = config.readOnly() || readOnly;

        if (readOnlyFinal) {
            query += " READ ONLY";
        }
        try (TryLock ignored = lock.get()) {
            sendQuery(query);
            interact();
        }
    }

    @SuppressWarnings("unused")
    public void commit () {
        try (TryLock ignored = lock.get()) {
            sendQuery("COMMIT");
            interact();
        }
    }

    @SuppressWarnings("unused")
    public void rollback () {
        try (TryLock ignored = lock.get()) {
            sendQuery("ROLLBACK");
            interact();
        }
    }

    @SuppressWarnings("unused")
    public boolean isIdle () {
        try (TryLock ignored = lock.get()) {
            return txStatus == TXStatus.IDLE;
        }
    }

    @SuppressWarnings("unused")
    public boolean isTxError () {
        try (TryLock ignored = lock.get()) {
            return txStatus == TXStatus.ERROR;
        }
    }

    @SuppressWarnings("unused")
    public boolean isTransaction () {
        try (TryLock ignored = lock.get()) {
            return txStatus == TXStatus.TRANSACTION;
        }
    }

    @SuppressWarnings("unused")
    public void setTxLevel (final TxLevel level) {
        try (TryLock ignored = lock.get()) {
            sendQuery(SQLTool.SQLSetTxLevel(level));
            interact();
        }
    }

    @SuppressWarnings("unused")
    public void setTxReadOnly () {
        try (TryLock ignored = lock.get()) {
            sendQuery(SQLTool.SQLSetTxReadOnly);
            interact();
        }
    }

    @SuppressWarnings("unused")
    public void listen (final String channel) {
        try (TryLock ignored = lock.get()) {
            query(String.format("listen %s", SQLTool.quoteChannel(channel)));
        }
    }

    @SuppressWarnings("unused")
    public void unlisten (final String channel) {
        try (TryLock ignored = lock.get()) {
            query(String.format("unlisten %s", SQLTool.quoteChannel(channel)));
        }
    }

    @SuppressWarnings("unused")
    public void notify (final String channel, final String message) {
        try (TryLock ignored = lock.get()) {
            final List<Object> params = List.of(channel, message);
            execute("select pg_notify($1, $2)", params);
        }
    }

}
