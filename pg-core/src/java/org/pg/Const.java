package org.pg;

import clojure.lang.Agent;
import org.pg.enums.SSLValidation;

import java.nio.charset.Charset;
import java.util.concurrent.Executor;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZoneOffset;

public final class Const {
    public static final int PROTOCOL_VERSION = 196608;
    public static final int CANCEL_CODE = 80877102;
    public static final int SSL_CODE = 80877103;
    public static final SSLValidation SSL_VALIDATION = SSLValidation.NONE;
    public static final int COPY_BUFFER_SIZE = 0xFFFF;
    public static final String COPY_FAIL_EXCEPTION_MSG = "Terminated due to an exception on the client side";
    public static final int PG_PORT = 5432;
    public static final boolean BIN_ENCODE = false;
    public static final boolean BIN_DECODE = false;
    public static final int SO_TIMEOUT = 15 * 1000;
    public static final int SO_RECV_BUF_SIZE = 0xFFFF;
    public static final int SO_SEND_BUF_SIZE = 0xFFFF;
    public static final String PG_HOST = "127.0.0.1";
    public static final int IN_STREAM_BUF_SIZE = 0xFFFF;
    public static final int OUT_STREAM_BUF_SIZE = 0xFFFF;
    public static final boolean SO_KEEP_ALIVE = true;
    public static final boolean SO_TCP_NO_DELAY = true;
    public static final long EXE_MAX_ROWS = 0xFFFFFFFFL;
    public static final int JSON_ENC_BUF_SIZE = 256;
    public static final String APP_NAME = "pg2";
    public static final String CLIENT_ENCODING = "UTF8";
    public static final char NULL_TAG = (char) 0;
    public static final String COPY_CSV_NULL = "";
    public static final String COPY_CSV_CELL_SEP = ",";
    public static final String COPY_CSV_CELL_QUOTE = "\"";
    public static final String COPY_CSV_LINE_SEP = "\r\n";
    public static final long MS_CANCEL_TIMEOUT = 1000 * 5;
    public static final byte JSONB_VERSION = 1;
    public static final char NULL_CHAR = (char)0;
    public static final int POOL_SIZE_MIN = 2;
    public static final int POOL_SIZE_MAX = 8;
    public static final int POOL_EXPIRE_THRESHOLD_MS = 1000 * 60 * 5;
    public static final int POOL_BORROW_CONN_TIMEOUT_MS = 1000 * 15;
    public static final Executor executor = Agent.soloExecutor;
    public static Charset serverCharset = StandardCharsets.UTF_8;
    public static Charset clientCharset = StandardCharsets.UTF_8;
    public static ZoneId timeZone = ZoneOffset.UTC;
    public static String dateStyle = "ISO, DMY";
    public static boolean integerDatetime = true;
//    public static boolean readPGTypes = true;
    public static String password = "";
    public static String defaultSchema = "public";
    public static boolean useSSL = false;
}
