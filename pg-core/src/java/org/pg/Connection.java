package org.pg;

import clojure.lang.Agent;
import clojure.lang.IFn;
import clojure.lang.IPersistentMap;
import clojure.lang.PersistentHashMap;
import org.pg.auth.MD5;
import org.pg.auth.ScramSha256;
import org.pg.clojure.LazyMap;
import org.pg.codec.EncoderBin;
import org.pg.codec.CodecParams;
import org.pg.codec.EncoderTxt;
import org.pg.copy.Copy;
import org.pg.enums.*;
import org.pg.error.PGError;
import org.pg.error.PGErrorResponse;
import org.pg.msg.*;
import org.pg.msg.client.*;
import org.pg.msg.server.*;
import org.pg.util.*;

import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.*;

import javax.net.ssl.SSLContext;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneId;
import java.util.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.function.Function;

public final class Connection implements AutoCloseable {

    private final Config config;
    private final UUID id;
    private final long createdAt;
    private int counter = 0;
    private final static System.Logger logger = System.getLogger(Pool.class.getCanonicalName());

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
    private AsynchronousSocketChannel socketChannel;
    private CompletableFuture<Object> asyncLock;
    private Executor executor;

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
        this.executor = Executors.newFixedThreadPool(16);
    }

    public static Connection connectSync (final Config config) {
        return null;
    }

    public static CompletableFuture<Connection> connect(final Config config, final boolean sendStartup) {
        final Connection conn = new Connection(config);
        return conn
                ._connect()
                .thenComposeAsync((Boolean ignored) -> {
                    if (sendStartup) {
                        return conn._authenticate();
                    }
                    else {
                        return CompletableFuture.completedFuture(conn);
                    }
                });
    }

    @SuppressWarnings("unused")
    public static CompletableFuture<Connection> connect(final Config config) {
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
                IOTool.close(socket);
                isClosed = true;
            }
        }
    }

    public CompletableFuture<Void> closeAsync() {
        return sendTerminate()
                .thenComposeAsync((Integer ignored) -> {
                    try {
                        socketChannel.close();
                        return CompletableFuture.completedFuture(null);
                    } catch (IOException e) {
                        return CompletableFuture.failedFuture(e);
                    }
                });
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

    private CompletableFuture<Connection> _authenticate () {
        return sendStartupMessage()
                .thenComposeAsync((Integer ignored) -> interact(true))
                .thenComposeAsync((Result ignored) -> CompletableFuture.completedFuture(this));
    }

    private CompletableFuture<Boolean> readSSLResponse () {
        return readNBytes(1).thenComposeAsync((ByteBuffer buf) -> {
            final char c = (char) IOTool.read(inStream);
            return switch (c) {
                case 'N' -> CompletableFuture.completedFuture(false);
                case 'S' -> CompletableFuture.completedFuture(true);
                default -> CompletableFuture.failedFuture(new PGError("wrong SSL response: %s", c));
            };
        });
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

//    private void upgradeToSSL () throws NoSuchAlgorithmException, IOException {
//        final SSLContext sslContext = getSSLContext();
//        final SSLSocket sslSocket = (SSLSocket) sslContext.getSocketFactory().createSocket(
//                socket,
//                config.host(),
//                config.port(),
//                true
//        );
//
//        final InputStream sslInStream = new BufferedInputStream(
//                IOTool.getInputStream(sslSocket),
//                config.inStreamBufSize()
//        );
//
//        final OutputStream sslOutStream = new BufferedOutputStream(
//                IOTool.getOutputStream(sslSocket),
//                config.outStreamBufSize()
//        );
//
//        sslSocket.setUseClientMode(true);
//        sslSocket.setEnabledProtocols(SSLProtocols);
//        sslSocket.startHandshake();
//
//        socket = sslSocket;
//        inStream = sslInStream;
//        outStream = sslOutStream;
//        isSSL = true;
//    }

//    private void _preSSLStage () {
//        if (config.useSSL()) {
//            final SSLRequest msg = new SSLRequest(Const.SSL_CODE);
//            sendMessage(msg);
//            final boolean ssl = readSSLResponse();
//            if (ssl) {
//                try {
//                    upgradeToSSL();
//                }
//                catch (Throwable e) {
//                    close();
//                    throw new PGError(
//                            e,
//                            "could not upgrade to SSL due to an exception: %s",
//                            e.getMessage()
//                    );
//                }
//            }
//            else {
//                close();
//                throw new PGError("the server is configured to not use SSL");
//            }
//        }
//    }

//    private void _connect () {
//        try (TryLock ignored = lock.get()) {
//            _connect_unlocked();
//        }
//    }

    private CompletableFuture<Boolean> _connect () {
        final int port = getPort();
        final String host = getHost();

        try {
            socketChannel = AsynchronousSocketChannel.open();
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }

        final InetSocketAddress address = new InetSocketAddress(host, port);
        final CompletableFuture<Boolean> future = new CompletableFuture<>();

        socketChannel.connect(address, future, new CompletionHandler<>() {
            @Override
            public void completed(Void result, CompletableFuture<Boolean> attachment) {
                attachment.complete(true);
            }
            @Override
            public void failed(Throwable exc, CompletableFuture<Boolean> attachment) {
                attachment.completeExceptionally(exc);
            }
        });

        return future;
    }

    private CompletableFuture<Integer> sendBytes(final byte[] buf) {
        if (Debug.isON) {
            Debug.debug(" < %s", Arrays.toString(buf));
        }
        return sendByteBuffer(ByteBuffer.wrap(buf));
    }

    // Like sendBytes above but taking boundaries into account.
    private void sendBytes (final byte[] buf, final int offset, final int len) {
        IOTool.write(outStream, buf, offset, len);
    }

    private CompletableFuture<Integer> sendBytesCopy(final byte[] bytes) {
        final ByteBuffer header = ByteBuffer.allocate(5);
        header.put((byte)'d');
        header.putInt(4 + bytes.length);
        return sendByteBuffer(header).thenComposeAsync((Integer ignored) -> sendBytes(bytes));

    }

    private CompletableFuture<Integer> sendByteBuffer(final ByteBuffer buf) {
        buf.rewind();
        final CompletableFuture<Integer> future = new CompletableFuture<>();
        socketChannel.write(buf, 0, new CompletionHandler<>() {
            @Override
            public void completed(Integer read, Integer total) {
                if (buf.hasRemaining()) {
                    socketChannel.write(buf, total + read, this);
                } else {
                    future.complete(total + read);
                }
            }
            @Override
            public void failed(Throwable exc, Integer total) {
                future.completeExceptionally(exc);
            }
        });
        return future;
    }

    private CompletableFuture<Integer> sendMessage(final IClientMessage msg) {
        if (Debug.isON) {
            Debug.debug(" <- %s", msg);
        }
        final ByteBuffer buf = msg.encode(codecParams.clientCharset());
        return sendByteBuffer(buf);
    }

    private String generateStatement () {
        return String.format("s%d", nextInt());
    }

    private String generatePortal () {
        return String.format("p%d", nextInt());
    }

    private CompletableFuture<Integer> sendStartupMessage () {
        final StartupMessage msg =
            new StartupMessage(
                    config.protocolVersion(),
                    config.user(),
                    config.database(),
                    config.pgParams()
            );
        return sendMessage(msg);
    }

    private CompletableFuture<Integer> sendCopyData (final byte[] buf) {
        return sendMessage(new CopyData(ByteBuffer.wrap(buf)));
    }

    private CompletableFuture<Integer> sendCopyDone () {
        return sendMessage(CopyDone.INSTANCE);
    }

    private CompletableFuture<Integer> sendCopyFail (final String errorMessage) {
        return sendMessage(new CopyFail(errorMessage));
    }

    private CompletableFuture<Integer> sendQuery (final String query) {
        return sendMessage(new Query(query));
    }

    private CompletableFuture<Integer> sendPasswordAsync (final String password) {
        return sendMessage(new PasswordMessage(password));
    }

    private CompletableFuture<Integer> sendPassword (final String password) {
        return sendMessage(new PasswordMessage(password));
    }

    private CompletableFuture<Integer> sendSync () {
        return sendMessage(Sync.INSTANCE);
    }

    private CompletableFuture<Integer> sendFlush () {
        return sendMessage(Flush.INSTANCE);
    }

    private CompletableFuture<Integer> sendTerminate () {
        return sendMessage(Terminate.INSTANCE);
    }


    @SuppressWarnings("unused")
    private CompletableFuture<Integer> sendSSLRequest () {
        return sendMessage(new SSLRequest(Const.SSL_CODE));
    }

    private CompletableFuture<ByteBuffer> readNBytes(final int size) {
        final ByteBuffer buf = ByteBuffer.allocate(size);
        final CompletableFuture<ByteBuffer> future = new CompletableFuture<>();
        socketChannel.read(buf, buf, new CompletionHandler<>() {
            @Override
            public void completed(Integer ignored, ByteBuffer buf) {
                if (buf.hasRemaining()) {
                    socketChannel.read(buf, buf, this);
                }
                else {
                    future.complete(buf.rewind());
                }
            }
            @Override
            public void failed(Throwable exc, ByteBuffer buf) {
                future.completeExceptionally(exc);
            }
        });
        return future;
    }

    private CompletableFuture<IServerMessage> readMessage () {
        return readNBytes(5).thenComposeAsync((ByteBuffer bbHeader) -> {
            final char tag = (char) bbHeader.get();
            final int bodySize = bbHeader.getInt() - 4;
            return readNBytes(bodySize).thenApplyAsync((ByteBuffer bbBody) -> switch (tag) {
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
            });
        });

    }

    private CompletableFuture<Integer> sendDescribeStatement (final String statement) {
        final Describe msg = new Describe(SourceType.STATEMENT, statement);
        return sendMessage(msg);
    }

    private CompletableFuture<Integer> sendDescribePortal (final String portal) {
        final Describe msg = new Describe(SourceType.PORTAL, portal);
        return sendMessage(msg);
    }

    private CompletableFuture<Integer> sendExecute (final String portal, final long maxRows) {
        final Execute msg = new Execute(portal, maxRows);
        return sendMessage(msg);
    }

    private synchronized CompletableFuture<Object> getAsyncLock(Function<? super Object,? extends CompletionStage<Object>> func) {
        if (asyncLock == null) {
            asyncLock = CompletableFuture.completedFuture(true).thenComposeAsync(func);
        }
        else {
            asyncLock = asyncLock.thenComposeAsync(func);
        }
        return asyncLock;
    }

    public CompletableFuture<Object> query(final String sql) {
        // return query(sql, ExecuteParams.INSTANCE);
        return getAsyncLock((Object ignored) -> query(sql, ExecuteParams.INSTANCE));
    }

    public CompletableFuture<Object> query(final String sql, final ExecuteParams executeParams) {
        return sendQuery(sql)
                .thenComposeAsync((Integer ignored) -> interact(executeParams))
                .thenComposeAsync((Result res) -> CompletableFuture.completedFuture(res.getResult()));
    };

    @SuppressWarnings("unused")
    public CompletableFuture<PreparedStatement> prepare (final String sql) {
        return prepare(sql, ExecuteParams.INSTANCE);
    }

    // public PreparedStatement prepare (final String sql, final ExecuteParams executeParams) {
//        try (TryLock ignored = lock.get()) {
//            return _prepare_unlocked(sql, executeParams);
//        }
   //  }

    private CompletableFuture<PreparedStatement>  prepare (
            final String sql,
            final ExecuteParams executeParams
    ) {
        final String statement = generateStatement();

        final List<OID> OIDsProvided = executeParams.OIDs();
        final int OIDsCount = OIDsProvided.size();
        final OID[] OIDs = new OID[OIDsCount];

        for (int i = 0; i < OIDsCount; i++) {
            OIDs[i] = OIDsProvided.get(i);
        }

        final Parse parse = new Parse(statement, sql, OIDs);
        return sendMessage(parse)
                .thenComposeAsync((Integer ignored) -> sendDescribeStatement(statement))
                .thenComposeAsync((Integer ignored) -> sendSync())
                .thenComposeAsync((Integer ignored) -> sendFlush())
                .thenComposeAsync((Integer ignored) -> interact())
                .thenComposeAsync((Result res) -> {
                    final ParameterDescription paramDesc = res.getParameterDescription();
                    final RowDescription rowDescription = res.getRowDescription();
                    final PreparedStatement stmt = new PreparedStatement(parse, paramDesc, rowDescription);
                    return CompletableFuture.completedFuture(stmt);
                });
    }

    private CompletableFuture<Integer> sendBind (
            final String portal,
            final PreparedStatement stmt,
            final ExecuteParams executeParams
    ) {
        final List<Object> params = executeParams.params();
        final OID[] OIDs = stmt.parameterDescription().OIDs();
        final int size = params.size();

        if (size != OIDs.length) {
            return CompletableFuture.failedFuture(new PGError(
                    "Wrong parameters count: %s (must be %s)",
                    size, OIDs.length
            ));
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
                    bytes[i] = value.getBytes(codecParams.clientCharset());
                }
            }
        }
        final Bind msg = new Bind(
                portal,
                statement,
                bytes,
                paramsFormat,
                columnFormat
        );

        return sendMessage(msg);

    }

    @SuppressWarnings("unused")
    public Object executeStatement(final PreparedStatement stmt) {
        return executeStatement(stmt, ExecuteParams.INSTANCE);
    }

    public CompletableFuture<Object> executeStatement (
            final PreparedStatement stmt,
            final ExecuteParams executeParams
    ) {
        final String portal = generatePortal();
        return sendBind(portal, stmt, executeParams)
                .thenComposeAsync((Integer ignored) -> sendDescribePortal(portal))
                .thenComposeAsync((Integer ignored) -> sendExecute(portal, executeParams.maxRows()))
                .thenComposeAsync((Integer ignored) -> sendClosePortal(portal))
                .thenComposeAsync((Integer ignored) -> sendSync())
                .thenComposeAsync((Integer ignored) -> sendFlush())
                .thenComposeAsync((Integer ignored) -> interact(executeParams))
                .thenComposeAsync((Result res) -> CompletableFuture.completedFuture(res.getResult()));
    }

    @SuppressWarnings("unused")
    public CompletableFuture<Object> execute (final String sql) {
        return execute(sql, ExecuteParams.INSTANCE);
    }

    public CompletableFuture<Object> execute (final String sql, final List<Object> params) {
        return execute(sql, ExecuteParams.builder().params(params).build());
    }

    public CompletableFuture<Object> execute (final String sql, final ExecuteParams executeParams) {
        final String portal = generatePortal();

        return prepare(sql, executeParams)
                .thenComposeAsync((PreparedStatement stmt) ->
                        sendBind(portal, stmt, executeParams)
                                .thenComposeAsync((Integer ignored) -> sendDescribePortal(portal))
                                .thenComposeAsync((Integer ignored) -> sendExecute(portal, executeParams.maxRows()))
                                .thenComposeAsync((Integer ignored) -> sendClosePortal(portal))
                                .thenComposeAsync((Integer ignored) -> sendCloseStatement(stmt)))
                .thenComposeAsync((Integer ignored) -> sendSync())
                .thenComposeAsync((Integer ignored) -> sendFlush())
                .thenComposeAsync((Integer ignored) -> interact(executeParams))
                .thenComposeAsync((Result res) -> CompletableFuture.completedFuture(res.getResult()));
    }

    private CompletableFuture<Integer> sendCloseStatement (final PreparedStatement stmt) {
        final Close msg = new Close(SourceType.STATEMENT, stmt.parse().statement());
        return sendMessage(msg);
    }

    private CompletableFuture<Integer> sendCloseStatement (final String statement) {
        final Close msg = new Close(SourceType.STATEMENT, statement);
        return sendMessage(msg);
    }

    private CompletableFuture<Integer> sendClosePortal (final String portal) {
        final Close msg = new Close(SourceType.PORTAL, portal);
        return sendMessage(msg);
    }

    public void closeStatement (final PreparedStatement statement) {
        closeStatement(statement.parse().statement());
    }

    public CompletableFuture<Boolean> closeStatement (final String statement) {
        return sendCloseStatement(statement)
                .thenComposeAsync((Integer ignored) -> sendSync())
                .thenComposeAsync((Integer ignored) -> sendFlush())
                .thenComposeAsync((Integer ignored) -> interact())
                .thenComposeAsync((Result res) -> CompletableFuture.completedFuture(true));

    }

    private CompletableFuture<Result> interact (final ExecuteParams executeParams) {
        return interact(executeParams, false);
    }

    private CompletableFuture<Result> interact(final Result resOld, final boolean isAuth) {
        return readMessage()
                .thenApplyAsync((IServerMessage msg) -> {
                    if (Debug.isON) {
                        Debug.debug(" -> %s", msg);
                    }
                    return msg;
                })
                .thenComposeAsync((IServerMessage msg) -> handleMessage(msg, resOld)
                        .thenComposeAsync((Result resNew) -> {
                            if (isEnough(msg, isAuth)) {
                                if (resNew.getErrorResponse() == null) {
                                    return CompletableFuture.completedFuture(resNew);
                                }
                                else {
                                    return CompletableFuture.failedFuture(new PGErrorResponse(resNew.getErrorResponse()));
                                }
                            }
                            else {
                                return interact(resNew, isAuth);
                            }
                        }));
    }

    private CompletableFuture<Result> interact (final ExecuteParams executeParams, final boolean isAuth) {
        final Result res = new Result(executeParams);
        return interact(res, isAuth);
    }


    private CompletableFuture<Result> interact (final boolean isAuth) {
        return interact(ExecuteParams.INSTANCE, isAuth);
    }

    private CompletableFuture<Result> interact () {
        return interact(ExecuteParams.INSTANCE, false);
    }

    // private static void noop () {}

//    private CompletableFuture<Result> handleMessageAsync(final IServerMessage msg, final Result res) {
//        // final CompletableFuture<Result> future = new CompletableFuture<>();
//
//        if (msg instanceof ParameterStatus x) {
//            handleParameterStatus(x);
//            return CompletableFuture.completedFuture(res);
//        } else if (msg instanceof AuthenticationMD5Password x) {
//            return handleAuthenticationMD5PasswordAsync(x)
//                    .thenComposeAsync((Integer ignored) -> CompletableFuture.completedFuture(res));
//
//    }

    private CompletableFuture<Result> handleMessage (final IServerMessage msg, final Result res) {

        if (msg instanceof DataRow x) {
            return handleDataRow(x, res);
        } else if (msg instanceof NotificationResponse x) {
            return handleNotificationResponse(x, res);
        } else if (msg instanceof AuthenticationCleartextPassword) {
            return handleAuthenticationCleartextPassword().thenComposeAsync((Integer ignored) -> CompletableFuture.completedFuture(res));
        } else if (msg instanceof AuthenticationSASL x) {
            return handleAuthenticationSASL(x, res);
        } else if (msg instanceof AuthenticationSASLContinue x) {
            return handleAuthenticationSASLContinue(x, res);
        } else if (msg instanceof AuthenticationSASLFinal x) {
            return handleAuthenticationSASLFinal(x, res);
        } else if (msg instanceof NoticeResponse x) {
            return handleNoticeResponse(x, res);
        } else if (msg instanceof ParameterStatus x) {
            return handleParameterStatus(x, res);
        } else if (msg instanceof RowDescription x) {
            return handleRowDescription(x, res);
        } else if (msg instanceof ReadyForQuery x) {
            return handleReadyForQuery(x, res);
        } else if (msg instanceof PortalSuspended x) {
            return handlePortalSuspended(x, res);
        } else if (msg instanceof AuthenticationMD5Password x) {
            return handleAuthenticationMD5Password(x, res);
        } else if (msg instanceof NegotiateProtocolVersion x) {
            return handleNegotiateProtocolVersion(x, res);
        } else if (msg instanceof CommandComplete x) {
            return handleCommandComplete(x, res);
        } else if (msg instanceof ErrorResponse x) {
            return handleErrorResponse(x, res);
        } else if (msg instanceof BackendKeyData x) {
            return handleBackendKeyData(x, res);
        } else if (msg instanceof ParameterDescription x) {
            return handleParameterDescription(x, res);
        } else if (msg instanceof ParseComplete x) {
            return CompletableFuture.completedFuture(res);
        } else if (msg instanceof CopyOutResponse x) {
            return CompletableFuture.completedFuture(res);
        } else if (msg instanceof CopyData x) {
            return handleCopyData(x, res);
        } else if (msg instanceof CopyInResponse) {
            return handleCopyInResponse(res);
        } else if (msg instanceof NoData) {
            return CompletableFuture.completedFuture(res);
        } else if (msg instanceof EmptyQueryResponse) {
            return CompletableFuture.completedFuture(res);
        } else if (msg instanceof CloseComplete) {
            return CompletableFuture.completedFuture(res);
        } else if (msg instanceof BindComplete) {
            return CompletableFuture.completedFuture(res);
        } else if (msg instanceof AuthenticationOk) {
            return CompletableFuture.completedFuture(res);
        } else if (msg instanceof CopyDone) {
            return CompletableFuture.completedFuture(res);
        } else if (msg instanceof SkippedMessage) {
            return CompletableFuture.completedFuture(res);
        } else {
            return CompletableFuture.failedFuture(new PGError("Cannot handle this message: %s", msg));
        }
    }

    private CompletableFuture<Result> handleAuthenticationSASL (final AuthenticationSASL msg, final Result res) {

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
            return sendMessage(msgSASL).thenComposeAsync((Integer ignored) -> CompletableFuture.completedFuture(res));
        }

        if (msg.isScramSha256Plus()) {
            return CompletableFuture.failedFuture(new PGError("SASL SCRAM SHA 256 PLUS method is not implemented yet"));
        }

        return CompletableFuture.failedFuture(new PGError("Unknown algo"));
    }

    private CompletableFuture<Result> handleAuthenticationSASLContinue (final AuthenticationSASLContinue msg, final Result res) {
        final ScramSha256.Step1 step1 = res.scramPipeline.step1;
        final String serverFirstMessage = msg.serverFirstMessage();
        final ScramSha256.Step2 step2 = ScramSha256.step2_serverFirstMessage(serverFirstMessage);
        final ScramSha256.Step3 step3 = ScramSha256.step3_clientFinalMessage(step1, step2);
        res.scramPipeline.step2 = step2;
        res.scramPipeline.step3 = step3;
        final SASLResponse msgSASL = new SASLResponse(step3.clientFinalMessage());
        return sendMessage(msgSASL).thenComposeAsync((Integer ignored) -> CompletableFuture.completedFuture(res));
    }

    private CompletableFuture<Result> handleAuthenticationSASLFinal (final AuthenticationSASLFinal msg, final Result res) {
        final String serverFinalMessage = msg.serverFinalMessage();
        final ScramSha256.Step4 step4 = ScramSha256.step4_serverFinalMessage(serverFinalMessage);
        res.scramPipeline.step4 = step4;
        final ScramSha256.Step3 step3 = res.scramPipeline.step3;
        try {
            ScramSha256.step5_verifyServerSignature(step3, step4);
            return CompletableFuture.completedFuture(res);
        } catch (Throwable e) {
            return CompletableFuture.failedFuture(e);
        }
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

    private CompletableFuture<Result> handleCopyInResponse (Result res) {

        throw new PGError("ccc");

        // These three methods only send data but do not read.
        // Thus, we rely on sendBytes which doesn't trigger flushing
        // the output stream. Flushing is expensive and thus must be called
        // manually when all the data has been sent.
//        if (res.executeParams.isCopyInRows()) {
//            handleCopyInResponseRows(res);
//        }
//        else if (res.executeParams.isCopyInMaps()) {
//            handleCopyInResponseMaps(res);
//        } else {
//            handleCopyInResponseStream(res);
//        }
        // Finally, we flush the output stream so all unsent bytes get sent.

    }

    private CompletableFuture<Result> handlePortalSuspended (final PortalSuspended msg, final Result res) {
        res.handlePortalSuspended(msg);
        return CompletableFuture.completedFuture(res);
    }

    private void handlerCall (final IFn f, final Object arg) {
        if (f == null) {
            logger.log(System.Logger.Level.INFO, "background handler call, arg: {0}", arg);
        } else {
            Agent.soloExecutor.submit(() -> f.invoke(arg));
        }
    }

    private CompletableFuture<Result> handleNotificationResponse (final NotificationResponse msg, final Result res) {
        handlerCall(config.fnNotification(), msg.toClojure());
        return CompletableFuture.completedFuture(res);
    }

    private CompletableFuture<Result> handleNoticeResponse (final NoticeResponse msg, final Result res) {
        handlerCall(config.fnNotice(), msg.toClojure());
        return CompletableFuture.completedFuture(res);
    }

    private CompletableFuture<Result> handleNegotiateProtocolVersion (
            final NegotiateProtocolVersion msg,
            final Result res
    ) {
        handlerCall(config.fnProtocolVersion(), msg.toClojure());
        return CompletableFuture.completedFuture(res);
    }


    private CompletableFuture<Result> handleAuthenticationMD5Password (
            final AuthenticationMD5Password msg,
            final Result res
    ) {
        final String hashed = MD5.hashPassword(
                config.user(),
                config.password(),
                msg.salt()
        );
        return sendPassword(hashed)
                .thenComposeAsync((Integer ignored) -> CompletableFuture.completedFuture(res));
    }

    private CompletableFuture<Result> handleCopyData (final CopyData msg, final Result res) {
        throw new PGError("aaa");
//        try {
//            handleCopyDataUnsafe(msg, res);
//        } catch (Throwable e) {
//            res.setException(e);
//        }
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
    public CompletableFuture<Object> copy (final String sql, final ExecuteParams executeParams) {
        return sendQuery(sql)
                .thenComposeAsync((Integer ignored) -> interact(executeParams))
                .thenComposeAsync((Result res) -> CompletableFuture.completedFuture(res.getResult()));
    }

    private static List<Object> mapToRow (final Map<?,?> map, final List<Object> keys) {
        final List<Object> row = new ArrayList<>(keys.size());
        for (final Object key: keys) {
            row.add(map.get(key));
        }
        return row;
    }

    private CompletableFuture<Result> handleParameterDescription (final ParameterDescription msg, final Result res) {
        res.handleParameterDescription(msg);
        return CompletableFuture.completedFuture(res);
    }

    private CompletableFuture<Integer> handleAuthenticationCleartextPassword () {
        return sendPassword(config.password());
    }

    private CompletableFuture<Result> handleParameterStatus (final ParameterStatus msg, final Result res) {
        setParam(msg.param(), msg.value());
        return CompletableFuture.completedFuture(res);
    }

    private static CompletableFuture<Result> handleRowDescription (final RowDescription msg, final Result res) {
        res.handleRowDescription(msg);
        return CompletableFuture.completedFuture(res);
    }

    private CompletableFuture<Result> handleDataRow (final DataRow msg, final Result res) {
        final RowDescription rowDescription = res.getRowDescription();
        final Map<Object, Short> keysIndex = res.getCurrentKeysIndex();
        final LazyMap lazyMap = new LazyMap(
                lock,
                msg,
                rowDescription,
                keysIndex,
                codecParams
        );
        res.addClojureRow(lazyMap);
        return CompletableFuture.completedFuture(res);
    }

//    private void handleDataRow (final DataRow msg, final Result res) {
//        try {
//            handleDataRowUnsafe(msg, res);
//        }
//        catch (Throwable e) {
//            res.setException(e);
//        }
//    }

    private CompletableFuture<Result> handleReadyForQuery (final ReadyForQuery msg, final Result res) {
        txStatus = msg.txStatus();
        return CompletableFuture.completedFuture(res);
    }

    private static CompletableFuture<Result> handleCommandComplete (final CommandComplete msg, final Result res) {
        res.handleCommandComplete(msg);
        return CompletableFuture.completedFuture(res);
    }

    private static CompletableFuture<Result> handleErrorResponse (final ErrorResponse msg, final Result res) {
        res.addErrorResponse(msg);
        return CompletableFuture.completedFuture(res);
    }

    private CompletableFuture<Result> handleBackendKeyData (final BackendKeyData msg, final Result res) {
        pid = msg.pid();
        secretKey = msg.secretKey();
        return CompletableFuture.completedFuture(res);
    }

    private static Boolean isEnough (final IServerMessage msg, final boolean isAuth) {
        return switch (msg.getClass().getSimpleName()) {
            case "ReadyForQuery" -> true;
            case "ErrorResponse" -> isAuth;
            default -> false;
        };
    }

    @SuppressWarnings("unused")
    public static CompletableFuture<Connection> clone (final Connection conn) {
        return Connection.connect(conn.config);
    }

    @SuppressWarnings("unused")
    public static CompletableFuture<Void> cancelRequest(final Connection conn) {
        final CancelRequest msg = new CancelRequest(Const.CANCEL_CODE, conn.pid, conn.secretKey);
        return Connection.connect(conn.config, false)
                .thenComposeAsync((Connection conn2) ->
                        conn2.sendMessage(msg)
                                .thenComposeAsync((Integer ignored) ->conn2.closeAsync()));
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
