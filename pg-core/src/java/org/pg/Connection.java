package org.pg;

import clojure.lang.*;
import org.pg.auth.MD5;
import org.pg.auth.ScramSha256;
import org.pg.clojure.KW;
import org.pg.clojure.RowMap;
import org.pg.codec.CodecParams;
import org.pg.enums.*;
import org.pg.error.PGError;
import org.pg.error.PGErrorResponse;
import org.pg.json.JSON;
import org.pg.msg.*;
import org.pg.msg.client.*;
import org.pg.msg.server.*;
import org.pg.processor.IProcessor;
import org.pg.type.PGType;
import org.pg.util.*;

import javax.net.ssl.SSLContext;
import java.net.UnixDomainSocketAddress;
import java.security.MessageDigest;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.time.ZoneId;
import java.util.*;
import java.nio.ByteBuffer;

public final class Connection implements AutoCloseable {

    private final Config config;
    private final UUID id;
    private final long createdAt;

    private int pid;
    private int secretKey;
    private TXStatus txStatus;
    private PGIOChannel ioChannel;
    private String unixSocketPath;
    private InputStream inStream;
    private OutputStream outStream;
    private final Map<String, String> params;
    private final CodecParams codecParams;
    private boolean isSSL = false;
    private final TryLock lock = new TryLock();
    private boolean isClosed = false;
    private final Map<String, PreparedStatement> PSCache;
    private final List<Object> notifications = new ArrayList<>(0);
    private final List<Object> notices = new ArrayList<>(0);

    @Override
    public boolean equals (Object other) {
        return other instanceof Connection && id.equals(((Connection) other).id);
    }

    @Override
    public int hashCode () {
        return this.id.hashCode();
    }

    private Connection(final Config config) {
        final CodecParams codecParams = CodecParams.create();
        codecParams.objectMapper(config.objectMapper());
        this.config = config;
        this.params = new HashMap<>();
        this.codecParams = codecParams;
        this.id = UUID.randomUUID();
        this.createdAt = System.currentTimeMillis();
        this.PSCache = new HashMap<>();
    }

    public static Connection connect(final Config config, final boolean sendStartup) {
        final Connection conn = new Connection(config);
        final ConnType connType = config.getConnType();
        switch (connType) {
            case UNIX_SOCKET -> conn.connectUnixSocket();
            case INET4 -> conn.connectInet();
        }
        if (sendStartup) {
            conn.authenticate();
//            if (config.readPGTypes()) {
//                try {
//                     conn.processTypeMap(); TODO?
//                } catch (Exception e) {
//                    conn.close();
//                    throw new PGError(e, "failed to preprocess postgres types, reason: %s", e.getMessage());
//                }
//            }
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

    private void closeIO() {
        IOTool.close(inStream);
        IOTool.close(outStream);
        isClosed = true;
    }

    public void close () {
        try (TryLock ignored = lock.get()) {
            if (!isClosed) {
                sendTerminate();
                flush();
                closeIO();
            }
        }
    }

    private static String getUnixSocketPath(final Config config) {
        final String path = config.unixSocketPath();
        if (path == null) {
            final int port = config.port();
            return SysTool.guessUnixSocketPath(port);
        } else {
            return path;
        }
    }

    private void setInputStream(final InputStream in) {
        final int len = config.inStreamBufSize();
        inStream = IOTool.wrapBuf(in, len);
    }

    private void setOutputStream(final OutputStream out) {
        final int len = config.outStreamBufSize();
        outStream = IOTool.wrapBuf(out, len);
    }

    private void connectStreams() {
        setInputStream(ioChannel.getInputStream());
        setOutputStream(ioChannel.getOutputStream());
    }

    public void connectUnixSocket() {
        final String path = getUnixSocketPath(config);
        final File file = new File(path);
        if (!file.exists()) {
            throw new PGError("unix socket doesn't exist: %s", path);
        }
        this.unixSocketPath = path;
        this.ioChannel = PGDomainSocketChannel.connect(UnixDomainSocketAddress.of(path));
        connectStreams();
    }

    @SuppressWarnings("unused")
    public PGType getPGTypeByName(final Object type) {
        final String fullName = CodecParams.objectToPGType(type);
        return codecParams.getPgType(fullName);
    }

    /*
    Override some oids with custom processors, if set.
     */
//    private void processTypeMap() {
//        final Map<Object, IProcessor> typeMap = config.typeMap();
//        if (typeMap == null) return;
//        for (Map.Entry<Object, IProcessor> me: typeMap.entrySet()) {
//            codecParams.setProcessor(me.getKey(), me.getValue());
//        }
//    }

    /*
    Return a string of comma-separated integer oids,
    skipping those that are equal to 0.
     */
    private static String joinOids(final Set<Integer> oids) {
        final StringBuilder sb = new StringBuilder();
        boolean firstBeen = false;
        for (int oid: oids) {
            if (oid != 0) {
                if (firstBeen) {
                    sb.append(',');
                }
                sb.append(oid);
                if (!firstBeen) {
                    firstBeen = true;
                }
            }

        }
        return sb.toString();
    }

    /*
    Fill-in the current CodecParams instance with postgres types. This data
    helps to guess how to process custom types shipped by extensions (and
    enums as well).
     */
    public void readTypesByOIDs(final Set<Integer> oids) {
        /*
        Below, we use ::text coercion because it's a special REGPROC
        type (oid 24). In binary mode, it gets passed as an integer
        (but as a string in text mode).

        We also exclude predefined types because we know their properties
        in advance.
        */
        if (oids.isEmpty()) {
            return;
        }
        final String query = """
copy (
    select
        pg_type.oid,
        pg_type.typname,
        pg_type.typtype,
        pg_type.typinput::text,
        pg_type.typoutput::text,
        pg_type.typreceive::text,
        pg_type.typsend::text,
        pg_type.typarray,
        pg_type.typdelim,
        pg_type.typelem,
        pg_namespace.nspname
    from
        pg_type,
        pg_namespace
    where
        pg_type.oid in (""" + joinOids(oids) + "+)" + """
        and pg_type.typnamespace = pg_namespace.oid
) to stdout with (format binary)
""";

        sendQuery(query);
        flush();

        IServerMessage msg;
        PGType pgType;
        ByteBuffer bb;
        boolean headerSeen = false;

        while (true) {
            msg = readMessage(false);
            if (Debug.isON) {
                Debug.debug(" -> %s", msg);
            }
            if (msg instanceof CopyData copyData) {
                bb = copyData.buf();
                if (!headerSeen) {
                    BBTool.skip(bb, Copy.COPY_BIN_HEADER.length);
                    headerSeen = true;
                }
                if (Copy.isTerminator(bb)) {
                    continue;
                }
                pgType = PGType.fromCopyBuffer(bb);
                codecParams.setPgType(pgType);
                // these messages are expected but just skipped
            } else if (msg instanceof CopyOutResponse) {
            } else if (msg instanceof CopyDone) {
            } else if (msg instanceof CommandComplete) {
            } else if (msg instanceof ReadyForQuery) {
                break;
            } else {
                throw new PGError("Unexpected message in readTypes: %s", msg);
            }
        }
    }

    @SuppressWarnings("unused")
    public int getPid () {
        try (TryLock ignored = lock.get()) {
            return pid;
        }
    }

    public UUID getId () {
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
            case "client_encoding" -> codecParams.clientCharset(Charset.forName(value));
            case "server_encoding" -> codecParams.serverCharset(Charset.forName(value));
            case "DateStyle" -> codecParams.dateStyle(value);
            case "TimeZone" -> codecParams.timeZone(ZoneId.of(value));
            case "integer_datetimes" -> codecParams.integerDatetime(value.equals("on"));
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
        final ConnType connType = config.getConnType();
        return switch (connType) {
            case UNIX_SOCKET -> String.format("<PG connection %s:%s %s>",
                    getUser(),
                    getDatabase(),
                    unixSocketPath
            );
            case INET4 -> String.format("<PG connection %s@%s:%s/%s>",
                    getUser(),
                    getHost(),
                    getPort(),
                    getDatabase()
            );
        };
    }

    private void authenticate() {
        sendStartupMessage();
        interactStartup();
    }

    private boolean readSSLResponse () {
        final char c = (char) IOTool.read(inStream);
        return switch (c) {
            case 'N' -> false;
            case 'S' -> true;
            default -> throw new PGError("wrong SSL response: %s", c);
        };
    }


    private SSLContext getSSLContext() {
        final SSLContext configContext = config.sslContext();
        if (configContext == null) {
            return switch (config.sslValidation()) {
                case NONE -> SSLTool.SSLContextNoValidation();
                case DEFAULT -> SSLTool.SSLContextDefault();
            };
        }
        else {
            return configContext;
        }
    }

    private void upgradeToSSL() {
        final SSLContext sslContext = getSSLContext();
        this.ioChannel = ioChannel.upgradeToSSL(sslContext);
        connectStreams();
        isSSL = true;
    }

    private void preSSLStage() {
        final SSLRequest msg = new SSLRequest(Const.SSL_CODE);
        sendMessage(msg);
        flush();
        final boolean ssl = readSSLResponse();
        if (ssl) {
            try {
                upgradeToSSL();
            }
            catch (final Throwable e) {
                closeIO();
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

    private void connectInet() {
        try (TryLock ignored = lock.get()) {
            connectInetUnlocked();
        }
    }

    private void connectInetUnlocked() {
        this.ioChannel = PGSocketChannel.connect(config);
        connectStreams();
        if (config.useSSL()) {
            preSSLStage();
        }
    }

    // Send bytes into the output stream. Do not flush the buffer,
    // must be done manually.
    private void sendBytes (final byte[] buf, final String tag) {
        if (Debug.isON) {
            Debug.debug(" <- sendBytes (%s): %s", tag, Arrays.toString(buf));
        }
        IOTool.write(outStream, buf);
    }

    // Like sendBytes above but taking boundaries into account.
    private void sendBytes (final byte[] buf, final int len) {
        IOTool.write(outStream, buf, 0, len);
    }

    private void sendBytesCopy(final byte[] bytes) {
        final ByteBuffer bb = ByteBuffer.allocate(5);
        bb.put((byte)'d');
        bb.putInt(4 + bytes.length);
        sendBytes(bb.array(), "COPY");
        sendBytes(bytes, "COPY");
    }

    private void sendMessage (final IClientMessage msg) {
        if (Debug.isON) {
            Debug.debug(" <- %s", msg);
        }
        final ByteBuffer buf = msg.encode(codecParams.clientCharset());
        IOTool.write(outStream, buf.array());
    }

    private String generateStatement () {
        return String.format("s%d", System.nanoTime());
    }

    private String generatePortal () {
        return String.format("p%d", System.nanoTime());
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

    @SuppressWarnings("unused")
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
            return interact(executeParams, sql).getResult();
        }
    }

    @SuppressWarnings("unused")
    public PreparedStatement prepare (final String sql) {
        return prepare(sql, ExecuteParams.INSTANCE);
    }

    public PreparedStatement prepare (final String sql, final ExecuteParams executeParams) {
        try (TryLock ignored = lock.get()) {
            return prepareUnlocked(sql, executeParams);
        }
    }

    private void readTypesAfterStatement(final int[] rowDescOids, final int[] paramDescOids) {
        final Set<Integer> oids = new HashSet<>();
        IProcessor processor;
        for (int oid: rowDescOids) {
            processor = codecParams.getProcessor(oid);
            if (processor.isUnsupported()) {
                oids.add(oid);
            }
        }
        for (int oid: paramDescOids) {
            processor = codecParams.getProcessor(oid);
            if (processor.isUnsupported()) {
                oids.add(oid);
            }
        }
        readTypesByOIDs(oids);
    }

    private void readTypesBeforeStatement(final ExecuteParams executeParams) {
        
    }

    private PreparedStatement prepareUnlocked(
            final String sql,
            final ExecuteParams executeParams
    ) {
        // TODO: read type name oids; refresh the types
        readTypesBeforeStatement(executeParams);
        final String statement = generateStatement();
        final int[] intOids = executeParams.getIntOids(codecParams);
        final Parse parse = new Parse(statement, sql, intOids);
        sendMessage(parse);
        sendDescribeStatement(statement);
        sendFlush();
        sendSync();
        final Result res = interact(sql);
        final ParameterDescription paramDesc = res.getParameterDescription();
        final RowDescription rowDescription = res.getRowDescription();
        readTypesAfterStatement(rowDescription.typeOids(), paramDesc.OIDs());
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
            sendBytes(buf, "BIND");
        }
    }

    /*
    Flush the output stream meaning all the buffered bytes
    get sent to a socket forcibly. Must be called *before*
    reading something from the service back.
     */
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
        final String sql = stmt.parse().query();
        try (TryLock ignored = lock.get()) {
            final String portal = generatePortal();
            sendBind(portal, stmt, executeParams);
            sendDescribePortal(portal);
            sendExecute(portal, executeParams.maxRows());
            sendClosePortal(portal);
            sendFlush();
            sendSync();
            return interact(executeParams, sql).getResult();
        }
    }

    @SuppressWarnings("unused")
    public int closeCachedPreparedStatements() {
        try (TryLock ignored = lock.get()) {
            final int len = PSCache.size();
            if (len > 0) {
                for (Map.Entry<String, PreparedStatement>entry: PSCache.entrySet()) {
                    if (Debug.isON) {
                        Debug.debug("Closing cached statement: %s", entry.getValue());
                    }
                    sendCloseStatement(entry.getValue());
                }
                sendFlush();
                sendSync();
                interact("--clear prepared statement cache");
                PSCache.clear();
            }
            return len;
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
            PreparedStatement stmt = PSCache.get(sql);
            if (stmt == null) {
                if (Debug.isON) {
                    Debug.debug("Prepared statement not found: %s", sql);
                }
                stmt = prepare(sql, executeParams);
                PSCache.put(sql, stmt);
            } else {
                if (Debug.isON) {
                    Debug.debug("Prepared statement found in cache: %s", stmt);
                }
            }
            final String portal = generatePortal();
            sendBind(portal, stmt, executeParams);
            sendDescribePortal(portal);
            sendExecute(portal, executeParams.maxRows());
            sendClosePortal(portal);
            // sendCloseStatement(stmt); // don't close it for caching
            sendFlush();
            sendSync();
            try {
                return interact(executeParams, sql).getResult();
            } catch (final PGErrorResponse e) {
                if (Objects.equals(e.getCode(), ErrCode.PREPARED_STATEMENT_NOT_FOUND)) {
                    if (Debug.isON) {
                        Debug.debug("Prepared statement is missing: %s, error: %s",
                                stmt,
                                e.getMessage()
                        );
                    }
                    PSCache.remove(sql);
                    return execute(sql, executeParams);
                } else {
                    throw e;
                }
            }
        }
    }

    @SuppressWarnings("unused")
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
        final String sql = String.format("--closing statement %s", statement);
        try (TryLock ignored = lock.get()) {
            sendCloseStatement(statement);
            sendFlush();
            sendSync();
            interact(sql);
        }
    }

    private Result interact (final ExecuteParams executeParams, final String sql) {
        return interact(executeParams, false, sql);
    }

    private Result interact (final ExecuteParams executeParams, final boolean isAuth, final String sql) {
        flush();
        final Result res = new Result(executeParams, sql);
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

    private void interactStartup () {
        interact(ExecuteParams.INSTANCE, true, null);
    }

    private Result interact (final String sql) {
        return interact(ExecuteParams.INSTANCE, false, sql);
    }

    private static void noop () {}

    private void handleMessage (final IServerMessage msg, final Result res) {

        if (msg instanceof final DataRow x) {
            handleDataRow(x, res);
        } else if (msg instanceof final NotificationResponse x) {
            handleNotificationResponse(x, res);
        } else if (msg instanceof AuthenticationCleartextPassword) {
            handleAuthenticationCleartextPassword();
        } else if (msg instanceof final AuthenticationSASL x) {
            handleAuthenticationSASL(x, res);
        } else if (msg instanceof final AuthenticationSASLContinue x) {
            handleAuthenticationSASLContinue(x, res);
        } else if (msg instanceof final AuthenticationSASLFinal x) {
            handleAuthenticationSASLFinal(x, res);
        } else if (msg instanceof final NoticeResponse x) {
            handleNoticeResponse(x);
        } else if (msg instanceof final ParameterStatus x) {
            handleParameterStatus(x);
        } else if (msg instanceof final RowDescription x) {
            handleRowDescription(x, res);
        } else if (msg instanceof final ReadyForQuery x) {
            handleReadyForQuery(x);
        } else if (msg instanceof final PortalSuspended x) {
            handlePortalSuspended(x, res);
        } else if (msg instanceof final AuthenticationMD5Password x) {
            handleAuthenticationMD5Password(x);
        } else if (msg instanceof final NegotiateProtocolVersion x) {
            handleNegotiateProtocolVersion(x);
        } else if (msg instanceof final CommandComplete x) {
            handleCommandComplete(x, res);
        } else if (msg instanceof final ErrorResponse x) {
            handleErrorResponse(x, res);
        } else if (msg instanceof final BackendKeyData x) {
            handleBackendKeyData(x);
        } else if (msg instanceof final ParameterDescription x) {
            handleParameterDescription(x, res);
        } else if (msg instanceof ParseComplete) {
            noop();
        } else if (msg instanceof CopyOutResponse) {
            noop();
        } else if (msg instanceof final CopyData x) {
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

    // stolen from ongres/scram, file TlsServerEndpoint.java
    private static MessageDigest getDigestAlgorithm(final String signatureAlgorithm) {
        final int index = signatureAlgorithm.indexOf("with");
        String algorithm = index > 0 ? signatureAlgorithm.substring(0, index) : "SHA-256";
        if (!algorithm.startsWith("SHA3-")) {
            algorithm = algorithm.replace("SHA", "SHA-");
        }
        if ("MD5".equals(algorithm) || "SHA-1".equals(algorithm)) {
            algorithm = "SHA-256";
        }
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (final NoSuchAlgorithmException e) {
            throw new PGError(e,
                    "no such digest algorithm: %s, source: %s",
                    algorithm, signatureAlgorithm
            );
        }
    }

    // stolen from jorsol/pgjdbc, file ScramAuthenticator.java
    private byte[] getChannelBindingData() {
        if (isSSL) {
            final Certificate peerCert = ioChannel.getPeerCertificate();
            if (peerCert instanceof final X509Certificate cert) {
                final String sigAlgName = cert.getSigAlgName();
                final MessageDigest digest = getDigestAlgorithm(sigAlgName);
                try {
                    return digest.digest(cert.getEncoded());
                } catch (final CertificateEncodingException e) {
                    throw new PGError("cannot get encoded payload of certificate: %s", cert);
                }
            } else {
                throw new PGError("certificate %s is not X509Certificate: %s", peerCert);
            }
        } else {
            throw new PGError("cannot get channel binding data because connection is not SSL");
        }
    }

    private void handleAuthenticationSASL (final AuthenticationSASL msg, final Result res) {

        res.scramPipeline = ScramSha256.pipeline();

        ScramSha256.BindFlag bindFlag;
        ScramSha256.BindType bindType;
        byte[] bindData;
        SASL saslType;

        if (msg.isScramSha256()) {
            bindFlag = ScramSha256.BindFlag.NO;
            bindType = ScramSha256.BindType.NONE;
            bindData = new byte[0];
            saslType = SASL.SCRAM_SHA_256;

        } else if (msg.isScramSha256Plus()) {
            bindFlag = ScramSha256.BindFlag.REQUIRED;
            bindType = ScramSha256.BindType.TLS_SERVER_END_POINT;
            bindData = getChannelBindingData();
            saslType = SASL.SCRAM_SHA_256_PLUS;

        } else {
            throw new PGError("Unknown SCRAM algorithm: %s", msg);
        }

        final ScramSha256.Step1 step1 = ScramSha256.step1_clientFirstMessage(
                config.user(),
                config.password(),
                bindFlag,
                bindType,
                bindData
        );
        final SASLInitialResponse msgSASL = new SASLInitialResponse(
                saslType,
                step1.clientFirstMessage()
        );
        res.scramPipeline.step1 = step1;
        sendMessage(msgSASL);
        flush();
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

        @SuppressWarnings("resource")
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

            sendBytes(bbLead.array(), "COPY IN");
            sendBytes(buf, read);
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
                    catch (final Throwable caught) {
                        e = caught;
                        break;
                    }
                    final byte[] bytes = line.getBytes(StandardCharsets.UTF_8);
                    sendBytesCopy(bytes);
                }
                break;

            case BIN:
                sendCopyData(Copy.COPY_BIN_HEADER);
                ByteBuffer buf;
                while (rows.hasNext()) {
                    try {
                        buf = Copy.encodeRowBin(rows.next(), executeParams, codecParams);
                    }
                    catch (final Throwable caught) {
                        e = caught;
                        break;
                    }
                    sendBytesCopy(buf.array());
                }
                if (e == null) {
                    sendBytes(Copy.MSG_COPY_BIN_TERM, "COPY IN BIN");
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
        config.executor().execute(() -> f.invoke(arg));
    }

    private void handleNotificationResponse (final NotificationResponse msg, final Result res) {
        res.incNotificationCount();
        // Sometimes, it's important to know whether a notification
        // was triggered by the current connection or another.
        final boolean isSelf = msg.pid() == pid;
        final Object obj = msg.toClojure().assoc(KW.self_QMARK, isSelf);
        final IFn handler = config.fnNotification();
        if (handler == null) {
            notifications.add(obj);
        } else {
            handlerCall(handler, obj);
        }
    }

    private void handleNoticeResponse (final NoticeResponse msg) {
        final IFn handler = config.fnNotice();
        final Object obj = msg.toClojure();
        if (handler == null) {
            notices.add(obj);
        } else {
            handlerCall(handler, obj);
        }
    }

    @SuppressWarnings("unused")
    public boolean hasNotifications() {
        try (final TryLock ignored = lock.get()) {
            return !notifications.isEmpty();
        }
    }

    @SuppressWarnings("unused")
    public boolean hasNotices() {
        try (final TryLock ignored = lock.get()) {
            return !notices.isEmpty();
        }
    }

    @SuppressWarnings("unused")
    public IPersistentVector drainNotifications() {
        try (final TryLock ignored = lock.get()) {
            final IPersistentVector result = PersistentVector.create(notifications);
            notifications.clear();
            return result;
        }
    }

    @SuppressWarnings("unused")
    public IPersistentVector drainNotices() {
        try (final TryLock ignored = lock.get()) {
            final IPersistentVector result = PersistentVector.create(notices);
            notices.clear();
            return result;
        }
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
        } catch (final Throwable e) {
            res.setException(e);
        }
    }

    private void handleCopyDataUnsafe (final CopyData msg, final Result res) throws IOException {
        @SuppressWarnings("resource")
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
            final Result res = interact(executeParams, sql);
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

    private void handleRowDescription (final RowDescription msg, final Result res) {
        res.handleRowDescription(msg);
    }

    private void handleDataRowUnsafe (final DataRow msg, final Result res) {
        final RowDescription rowDescription = res.getRowDescription();
        final Map<Object, Short> keysIndex = res.getCurrentKeysIndex();
        final Object[] keys = res.getCurrentKeys();
        final RowMap rowMap = new RowMap(
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
        catch (final Throwable e) {
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
            interact(query);
        }
    }

    @SuppressWarnings("unused")
    public void commit () {
        final String sql = "COMMIT";
        try (TryLock ignored = lock.get()) {
            sendQuery(sql);
            interact(sql);
        }
    }

    @SuppressWarnings("unused")
    public void rollback () {
        final String sql = "ROLLBACK";
        try (TryLock ignored = lock.get()) {
            sendQuery("ROLLBACK");
            interact(sql);
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
        final String sql = SQLTool.SQLSetTxLevel(level);
        try (TryLock ignored = lock.get()) {
            sendQuery(sql);
            interact(sql);
        }
    }

    @SuppressWarnings("unused")
    public void setTxReadOnly () {
        final String sql = SQLTool.SQLSetTxReadOnly;
        try (TryLock ignored = lock.get()) {
            sendQuery(sql);
            interact(sql);
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

    @SuppressWarnings("unused")
    public void notifyJSON (final String channel, final Object data) {
        try (TryLock ignored = lock.get()) {
            final String payload = JSON.writeValueToString(config.objectMapper(), data);
            notify(channel, payload);
        }
    }

    @SuppressWarnings("unused")
    public int pollNotifications() {
        try (TryLock ignored = lock.get()) {
            final Result res = new Result(ExecuteParams.INSTANCE, "--pollNotifications");
            while (IOTool.available(inStream) > 0) {
                final IServerMessage msg = readMessage(res.hasException());
                if (   msg instanceof NotificationResponse
                    || msg instanceof NoticeResponse
                    || msg instanceof ParameterStatus) {
                    handleMessage(msg, res);
                } else {
                    throw new PGError("Unexpected message in pollNotifications: %s", msg);
                }
            }
            res.maybeThrowError();
            return res.getNotificationCount();
        }
    }

}
