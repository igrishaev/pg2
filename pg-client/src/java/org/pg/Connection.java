package org.pg;

import clojure.lang.Agent;
import clojure.lang.IFn;
import clojure.lang.IPersistentMap;
import clojure.lang.PersistentHashMap;
import org.pg.auth.MD5;
import org.pg.auth.ScramSha256;
import org.pg.codec.DecoderBin;
import org.pg.codec.DecoderTxt;
import org.pg.codec.EncoderBin;
import org.pg.codec.CodecParams;
import org.pg.codec.EncoderTxt;
import org.pg.copy.Copy;
import org.pg.enums.*;
import org.pg.msg.*;
import org.pg.type.OIDHint;
import org.pg.util.BBTool;
import org.pg.util.IOTool;
import org.pg.util.SQL;

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
import java.util.concurrent.atomic.AtomicInteger;

public class Connection implements Closeable {

    private static final boolean isDebug =
            System.getenv()
                    .getOrDefault("PG_DEBUG", "")
                    .equals("1");

    private final ConnConfig config;
    private final UUID id;
    private final long createdAt;
    private final AtomicInteger aInt;

    private int pid;
    private int secretKey;
    private TXStatus txStatus;
    private Socket socket;
    private InputStream inStream;
    private OutputStream outStream;
    private final Map<String, String> params;
    private final CodecParams codecParams;
    private boolean isSSL = false;
    private final static System.Logger.Level level = System.Logger.Level.INFO;
    private final System.Logger logger = System.getLogger(Connection.class.getCanonicalName());

    public Connection(final String host,
                      final int port,
                      final String user,
                      final String password,
                      final String database
    ) {
        this(ConnConfig.builder(user, database)
                .host(host)
                .port(port)
                .password(password)
                .build());
    }

    public Connection(final ConnConfig config, final boolean sendStartup) {
        this.config = config;
        this.params = new HashMap<>();
        this.codecParams = CodecParams.standard();
        this.id = UUID.randomUUID();
        this.createdAt = System.currentTimeMillis();
        this.aInt = new AtomicInteger();
        connect();
        setSocketOptions();
        preSSLStage();
        if (sendStartup) {
            authenticate();
        }
    }

    public Connection(final ConnConfig config) {
        this(config, true);
    }

    public void close () {
        if (!isClosed()) {
            sendTerminate();
            IOTool.close(socket);
        }
    }

    private void setSocketOptions () {
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

    private int nextInt() {
        return aInt.incrementAndGet();
    }

    public synchronized int getPid () {
        return pid;
    }

    public UUID getId() {
        return id;
    }

    @SuppressWarnings("unused")
    public long getCreatedAt() {
        return createdAt;
    }

    public synchronized Boolean isClosed () {
        return socket.isClosed();
    }

    @SuppressWarnings("unused")
    public synchronized TXStatus getTxStatus () {
        return txStatus;
    }

    @SuppressWarnings("unused")
    public synchronized boolean isSSL () {
        return isSSL;
    }

    @SuppressWarnings("unused")
    public synchronized String getParam (final String param) {
        return params.get(param);
    }

    @SuppressWarnings("unused")
    public synchronized IPersistentMap getParams () {
        return PersistentHashMap.create(params);
    }

    private void setParam (final String param, final String value) {
        params.put(param, value);
        switch (param) {
            case "client_encoding" ->
                    codecParams.clientCharset = Charset.forName(value);
            case "server_encoding" ->
                    codecParams.serverCharset = Charset.forName(value);
            case "DateStyle" ->
                    codecParams.dateStyle = value;
            case "TimeZone" ->
                    codecParams.timeZone = ZoneId.of(value);
            case "integer_datetimes" ->
                    codecParams.integerDatetime = value.equals("on");
        }
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

    public void authenticate () {
        sendStartupMessage();
        interact(Phase.AUTH);
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

    private void preSSLStage () {
        if (config.useSSL()) {
            final SSLRequest msg = new SSLRequest(Const.SSL_CODE);
            sendMessage(msg);
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

    private synchronized void connect () {
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
        if (isDebug) {
            logger.log(level," <- {0}", Arrays.toString(buf));
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

    // Send a message into the output stream. Flushes the stream forcibly.
    private void sendMessage (final IMessage msg) {
        if (isDebug) {
            logger.log(level, " <- {0}", msg);
        }
        final ByteBuffer buf = msg.encode(codecParams.clientCharset);
        IOTool.write(outStream, buf.array());
        IOTool.flush(outStream);
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

    private Object readMessage (final boolean skipMode) {

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
            case 'R' -> AuthenticationResponse.fromByteBuffer(bbBody).parseResponse(bbBody, codecParams.serverCharset);
            case 'S' -> ParameterStatus.fromByteBuffer(bbBody, codecParams.serverCharset);
            case 'Z' -> ReadyForQuery.fromByteBuffer(bbBody);
            case 'C' -> CommandComplete.fromByteBuffer(bbBody, codecParams.serverCharset);
            case 'T' -> RowDescription.fromByteBuffer(bbBody, codecParams.serverCharset);
            case 'D' -> DataRow.fromByteBuffer(bbBody);
            case 'E' -> ErrorResponse.fromByteBuffer(bbBody, codecParams.serverCharset);
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
            case 'v' -> NegotiateProtocolVersion.fromByteBuffer(bbBody, codecParams.serverCharset);
            case 'A' -> NotificationResponse.fromByteBuffer(bbBody, codecParams.serverCharset);
            case 'N' -> NoticeResponse.fromByteBuffer(bbBody, codecParams.serverCharset);
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

    private void sendExecute (final String portal, final long rowCount) {
        final Execute msg = new Execute(portal, rowCount);
        sendMessage(msg);
    }

    public synchronized Object query(final String sql) {
        return query(sql, ExecuteParams.INSTANCE);
    }

    public synchronized Object query(final String sql, final ExecuteParams executeParams) {
        sendQuery(sql);
        return interact(Phase.QUERY, executeParams).getResult();
    }

    public synchronized PreparedStatement prepare (final String sql, final ExecuteParams executeParams) {
        final String statement = generateStatement();

        final List<OID> OIDsProvided = executeParams.OIDs();
        final int OIDsProvidedCount = OIDsProvided.size();

        final List<Object> params = executeParams.params();
        final int paramCount = params.size();

        final OID[] OIDs = new OID[paramCount];

        for (int i = 0; i < paramCount; i++) {
            if (i < OIDsProvidedCount) {
                OIDs[i] = OIDsProvided.get(i);
            }
            else {
                Object param = params.get(i);
                OIDs[i] = OIDHint.guessOID(param);
            }
        }

        final Parse parse = new Parse(statement, sql, OIDs);
        sendMessage(parse);
        sendDescribeStatement(statement);
        sendSync();
        sendFlush();
        final Accum acc = interact(Phase.PREPARE);
        final ParameterDescription paramDesc = acc.getParameterDescription();
        return new PreparedStatement(parse, paramDesc);
    }

    private void sendBind (final String portal,
                           final PreparedStatement stmt,
                           final ExecuteParams executeParams
    ) {
        final List<Object> params = executeParams.params();
        final OID[] OIDs = stmt.parameterDescription().OIDs();
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
        int i = -1;
        for (final Object param: params) {
            i++;
            if (param == null) {
                bytes[i] = null;
                continue;
            }
            OID oid = OIDs[i];
            switch (paramsFormat) {
                case BIN -> {
                    ByteBuffer buf = EncoderBin.encode(param, oid, codecParams);
                    bytes[i] = buf.array();
                }
                case TXT -> {
                    String value = EncoderTxt.encode(param, oid, codecParams);
                    bytes[i] = value.getBytes(codecParams.clientCharset);
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
        sendMessage(msg);
    }

    public synchronized Object executeStatement (
            final PreparedStatement stmt,
            final ExecuteParams executeParams
    ) {
        final String portal = generatePortal();
        sendBind(portal, stmt, executeParams);
        sendDescribePortal(portal);
        sendExecute(portal, executeParams.rowCount());
        sendClosePortal(portal);
        sendSync();
        sendFlush();
        return interact(Phase.EXECUTE, executeParams).getResult();
    }

    public synchronized Object execute (final String sql) {
        return execute(sql, ExecuteParams.INSTANCE);
    }

    public synchronized Object execute (final String sql, final List<Object> params) {
        return execute(sql, ExecuteParams.builder().params(params).build());
    }

    public synchronized Object execute (final String sql, final ExecuteParams executeParams) {
        final PreparedStatement stmt = prepare(sql, executeParams);
        final Object res = executeStatement(stmt, executeParams);
        closeStatement(stmt);
        return res;
    }

    private void sendCloseStatement (final String statement) {
        final Close msg = new Close(SourceType.STATEMENT, statement);
        sendMessage(msg);
    }

    private void sendClosePortal (final String portal) {
        final Close msg = new Close(SourceType.PORTAL, portal);
        sendMessage(msg);
    }

    public synchronized void closeStatement (final PreparedStatement statement) {
        closeStatement(statement.parse().statement());
    }

    public synchronized void closeStatement (final String statement) {
        sendCloseStatement(statement);
        sendSync();
        sendFlush();
        interact(Phase.CLOSE);
    }

    private Accum interact(final Phase phase, final ExecuteParams executeParams) {
        final Accum acc = new Accum(phase, executeParams);
        while (true) {
            final Object msg = readMessage(acc.hasException());
            if (isDebug) {
                logger.log(level, " -> {0}", msg);
            }
            handleMessage(msg, acc);
            if (isEnough(msg, phase)) {
                break;
            }
        }
        acc.maybeThrowError();
        return acc;
    }

    private Accum interact(final Phase phase) {
        return interact(phase, ExecuteParams.INSTANCE);
    }

    private void handleMessage(final Object msg, final Accum acc) {
        switch (msg.getClass().getSimpleName()) {
            case
                    "NotificationResponse" ->
                    handleNotificationResponse((NotificationResponse)msg);
            case
                    "NoData",
                    "EmptyQueryResponse",
                    "CloseComplete",
                    "BindComplete",
                    "AuthenticationOk",
                    "CopyDone",
                    "SkippedMessage"-> {}
            case
                    "AuthenticationCleartextPassword" ->
                    handleAuthenticationCleartextPassword();
            case
                    "AuthenticationSASL" ->
                    handleAuthenticationSASL((AuthenticationSASL)msg, acc);
            case
                    "AuthenticationSASLContinue" ->
                    handleAuthenticationSASLContinue((AuthenticationSASLContinue)msg, acc);
            case
                    "AuthenticationSASLFinal" ->
                    handleAuthenticationSASLFinal((AuthenticationSASLFinal)msg, acc);
            case
                    "NoticeResponse" ->
                    handleNoticeResponse((NoticeResponse)msg);
            case
                    "ParameterStatus" ->
                    handleParameterStatus((ParameterStatus)msg);
            case
                    "RowDescription" ->
                    handleRowDescription((RowDescription)msg, acc);
            case
                    "DataRow" ->
                    handleDataRow((DataRow)msg, acc);
            case
                    "ReadyForQuery" ->
                    handleReadyForQuery((ReadyForQuery)msg);
            case
                    "PortalSuspended" ->
                    handlePortalSuspended((PortalSuspended)msg, acc);
            case
                    "AuthenticationMD5Password" ->
                    handleAuthenticationMD5Password((AuthenticationMD5Password)msg);
            case
                    "NegotiateProtocolVersion" ->
                    handleNegotiateProtocolVersion((NegotiateProtocolVersion)msg);
            case
                    "CommandComplete" ->
                    handleCommandComplete((CommandComplete)msg, acc);
            case
                    "ErrorResponse" ->
                    handleErrorResponse((ErrorResponse)msg, acc);
            case
                    "BackendKeyData" ->
                    handleBackendKeyData((BackendKeyData)msg);
            case
                    "ParameterDescription" ->
                    handleParameterDescription((ParameterDescription)msg, acc);
            case
                    "ParseComplete" ->
                    handleParseComplete((ParseComplete)msg, acc);
            case
                    "CopyOutResponse" ->
                    handleCopyOutResponse((CopyOutResponse)msg, acc);
            case
                    "CopyData" ->
                    handleCopyData((CopyData)msg, acc);
            case
                    "CopyInResponse" ->
                    handleCopyInResponse(acc);
            default ->
                    throw new PGError("Cannot handle this message: %s", msg);
        }
    }

    private void handleAuthenticationSASL(final AuthenticationSASL msg, final Accum acc) {

        acc.scramPipeline = ScramSha256.pipeline();

        if (msg.isScramSha256()) {
            final ScramSha256.Step1 step1 = ScramSha256.step1_clientFirstMessage(
                    config.user(), config.password()
            );
            final SASLInitialResponse msgSASL = new SASLInitialResponse(
                    SASL.SCRAM_SHA_256,
                    step1.clientFirstMessage()
            );
            acc.scramPipeline.step1 = step1;
            sendMessage(msgSASL);
        }

        if (msg.isScramSha256Plus()) {
            throw new PGError("SASL SCRAM SHA 256 PLUS method is not implemented yet");
        }
    }

    private void handleAuthenticationSASLContinue(final AuthenticationSASLContinue msg, final Accum acc) {
        final ScramSha256.Step1 step1 = acc.scramPipeline.step1;
        final String serverFirstMessage = msg.serverFirstMessage();
        final ScramSha256.Step2 step2 = ScramSha256.step2_serverFirstMessage(serverFirstMessage);
        final ScramSha256.Step3 step3 = ScramSha256.step3_clientFinalMessage(step1, step2);
        acc.scramPipeline.step2 = step2;
        acc.scramPipeline.step3 = step3;
        final SASLResponse msgSASL = new SASLResponse(step3.clientFinalMessage());
        sendMessage(msgSASL);
    }

    private void handleAuthenticationSASLFinal(final AuthenticationSASLFinal msg, final Accum acc) {
        final String serverFinalMessage = msg.serverFinalMessage();
        final ScramSha256.Step4 step4 = ScramSha256.step4_serverFinalMessage(serverFinalMessage);
        acc.scramPipeline.step4 = step4;
        final ScramSha256.Step3 step3 = acc.scramPipeline.step3;
        ScramSha256.step5_verifyServerSignature(step3, step4);
    }

    private void handleCopyInResponseStream(Accum acc) {

        final int bufSize = acc.executeParams.copyBufSize();
        final byte[] buf = new byte[bufSize];

        final ByteBuffer bbLead = ByteBuffer.allocate(5);
        bbLead.put((byte)'d');

        InputStream inputStream = acc.executeParams.inputStream();

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
            acc.setException(e);
            sendCopyFail(Const.COPY_FAIL_EXCEPTION_MSG);
        }
    }

    private void handleCopyInResponseData (final Accum acc, final Iterator<List<Object>> rows) {
        final ExecuteParams executeParams = acc.executeParams;
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
            acc.setException(e);
            sendCopyFail(Const.COPY_FAIL_EXCEPTION_MSG);
        }
    }

    private void handleCopyInResponseRows (final Accum acc) {
        final Iterator<List<Object>> iterator = acc.executeParams.copyInRows()
                .stream()
                .filter(Objects::nonNull)
                .iterator();
        handleCopyInResponseData(acc, iterator);
    }

    private void handleCopyInResponseMaps(final Accum acc) {
        final List<Object> keys = acc.executeParams.copyMapKeys();
        final Iterator<List<Object>> iterator = acc.executeParams.copyInMaps()
                .stream()
                .filter(Objects::nonNull)
                .map(map -> mapToRow(map, keys))
                .iterator();
        handleCopyInResponseData(acc, iterator);
    }

    private void handleCopyInResponse(Accum acc) {

        // These three methods only send data but do not read.
        // Thus, we rely on sendBytes which doesn't trigger flushing
        // the output stream. Flushing is expensive and thus must be called
        // manually when all the data has been sent.
        if (acc.executeParams.copyInRows() != null) {
            handleCopyInResponseRows(acc);
        }
        else if (acc.executeParams.copyInMaps() != null) {
            handleCopyInResponseMaps(acc);
        } else {
            handleCopyInResponseStream(acc);
        }
        // Finally, we flush the output stream so all unsent bytes get sent.
        IOTool.flush(outStream);
    }

    private void handlePortalSuspended(final PortalSuspended msg, final Accum acc) {
        acc.handlePortalSuspended(msg);
    }

    private static void futureCall(final IFn f, final Object arg) {
        Agent.soloExecutor.submit(() -> {
            f.invoke(arg);
        });
    }

    private void handleNotificationResponse(final NotificationResponse msg) {
        futureCall(config.fnNotification(), msg.toClojure());
    }

    private void handleNoticeResponse(final NoticeResponse msg) {
        futureCall(config.fnNotice(), msg.toClojure());
    }

    private void handleNegotiateProtocolVersion(final NegotiateProtocolVersion msg) {
        futureCall(config.fnProtocolVersion(), msg.toClojure());
    }

    private void handleAuthenticationMD5Password(final AuthenticationMD5Password msg) {
        final String hashed = MD5.hashPassword(
                config.user(),
                config.password(),
                msg.salt()
        );
        sendPassword(hashed);
    }

    private void handleCopyOutResponse(final CopyOutResponse msg, final Accum acc) {
        acc.handleCopyOutResponse(msg);
    }

    private void handleCopyData(final CopyData msg, final Accum acc) {
        try {
            handleCopyDataUnsafe(msg, acc);
        } catch (Throwable e) {
            acc.setException(e);
        }
    }

    private void handleCopyDataUnsafe(final CopyData msg, final Accum acc) throws IOException {
        final OutputStream outputStream = acc.executeParams.outputStream();
        final byte[] bytes = msg.buf().array();
        outputStream.write(bytes);
    }

    @SuppressWarnings("unused")
    public synchronized Object copy (final String sql, final ExecuteParams executeParams) {
        sendQuery(sql);
        final Accum acc = interact(Phase.COPY, executeParams);
        return acc.getResult();
    }

    private static List<Object> mapToRow(final Map<?,?> map, final List<Object> keys) {
        final List<Object> row = new ArrayList<>(keys.size());
        for (final Object key: keys) {
            row.add(map.get(key));
        }
        return row;
    }

    private void handleParseComplete(final ParseComplete msg, final Accum acc) {
        acc.handleParseComplete(msg);
    }

    private void handleParameterDescription (final ParameterDescription msg, final Accum acc) {
        acc.handleParameterDescription(msg);
    }

    private void handleAuthenticationCleartextPassword() {
        sendPassword(config.password());
    }

    private void handleParameterStatus(final ParameterStatus msg) {
        setParam(msg.param(), msg.value());
    }

    private static void handleRowDescription(final RowDescription msg, final Accum acc) {
        acc.handleRowDescription(msg);
    }

    private void handleDataRowUnsafe(final DataRow msg, final Accum acc) {
        final short size = msg.valueCount();
        final RowDescription.Column[] cols = acc.getRowDescription().columns();
        final ByteBuffer[] bufs = msg.values();
        final Object[] values = new Object[size];
        for (short i = 0; i < size; i++) {
            final ByteBuffer buf = bufs[i];
            if (buf == null) {
                values[i] = null;
                continue;
            }
            final RowDescription.Column col = cols[i];
            final Object value = switch (col.format()) {
                case TXT -> {
                    final String string = BBTool.getString(buf, codecParams.serverCharset);
                    yield DecoderTxt.decode(string, col.typeOid());
                }
                case BIN -> DecoderBin.decode(buf, col.typeOid(), codecParams);
            };
            values[i] = value;
        }
        acc.setCurrentValues(values);
    }

    private void handleDataRow(final DataRow msg, final Accum acc) {
        try {
            handleDataRowUnsafe(msg, acc);
        }
        catch (Throwable e) {
            acc.setException(e);
        }
    }

    private void handleReadyForQuery(final ReadyForQuery msg) {
        txStatus = msg.txStatus();
    }

    private static void handleCommandComplete(final CommandComplete msg, final Accum acc) {
        acc.handleCommandComplete(msg);
    }

    private static void handleErrorResponse(final ErrorResponse msg, final Accum acc) {
        acc.addErrorResponse(msg);
    }

    private void handleBackendKeyData(final BackendKeyData msg) {
        pid = msg.pid();
        secretKey = msg.secretKey();
    }

    private static Boolean isEnough (final Object msg, final Phase phase) {
        return switch (msg.getClass().getSimpleName()) {
            case "ReadyForQuery" -> true;
            case "ErrorResponse" -> phase == Phase.AUTH;
            default -> false;
        };
    }

    @SuppressWarnings("unused")
    public static Connection clone (final Connection conn) {
        return new Connection(conn.config);
    }

    @SuppressWarnings("unused")
    public static void cancelRequest(final Connection conn) {
        final CancelRequest msg = new CancelRequest(Const.CANCEL_CODE, conn.pid, conn.secretKey);
        final Connection temp = new Connection(conn.config, false);
        temp.sendMessage(msg);
        temp.close();
    }

    @SuppressWarnings("unused")
    public synchronized void begin () {
        sendQuery("BEGIN");
        interact(Phase.QUERY);
    }

    @SuppressWarnings("unused")
    public synchronized void commit () {
        sendQuery("COMMIT");
        interact(Phase.QUERY);
    }

    @SuppressWarnings("unused")
    public synchronized void rollback () {
        sendQuery("ROLLBACK");
        interact(Phase.QUERY);
    }

    @SuppressWarnings("unused")
    public synchronized boolean isIdle () {
        return txStatus == TXStatus.IDLE;
    }

    @SuppressWarnings("unused")
    public synchronized boolean isTxError () {
        return txStatus == TXStatus.ERROR;
    }

    @SuppressWarnings("unused")
    public synchronized boolean isTransaction () {
        return txStatus == TXStatus.TRANSACTION;
    }

    @SuppressWarnings("unused")
    public void setTxLevel (final TxLevel level) {
        sendQuery(SQL.SQLSetTxLevel(level));
        interact(Phase.QUERY);
    }

    @SuppressWarnings("unused")
    public void setTxReadOnly () {
        sendQuery(SQL.SQLSetTxReadOnly);
        interact(Phase.QUERY);
    }

    @SuppressWarnings("unused")
    public synchronized void listen (final String channel) {
        query(String.format("listen %s", SQL.quoteChannel(channel)));
    }

    @SuppressWarnings("unused")
    public synchronized void unlisten (final String channel) {
        query(String.format("unlisten %s", SQL.quoteChannel(channel)));
    }

    @SuppressWarnings("unused")
    public synchronized void notify (final String channel, final String message) {
        final ArrayList<Object> params = new ArrayList<>(2);
        params.add(channel);
        params.add(message);
        execute("select pg_notify($1, $2)", params);
    }

}
