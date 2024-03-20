package org.pg;

public final class Const {
    public static final int PROTOCOL_VERSION = 196608;
    public static final int CANCEL_CODE = 80877102;
    public static final int SSL_CODE = 80877103;
    public static final int COPY_BUFFER_SIZE = 0xFFFF;
    public static final String COPY_FAIL_EXCEPTION_MSG = "Terminated due to an exception on the client side";
    public static final int PG_PORT = 5432;
    public static final int SO_TIMEOUT = 15 * 1000;
    public static final int SO_RECV_BUF_SIZE = 0xFFFF;
    public static final int SO_SEND_BUF_SIZE = 0xFFFF;
    public static final String PG_HOST = "127.0.0.1";
    public static final int IN_STREAM_BUF_SIZE = 0xFFFF;
    public static final int OUT_STREAM_BUF_SIZE = 0xFFFF;
    public static final long EXE_MAX_ROWS = 0xFFFFFFFFL;
    public static final int JSON_ENC_BUF_SIZE = 256;
    public static final String APP_NAME = "pg2";
    public static final String CLIENT_ENCODING = "UTF8";
    public static final char NULL_TAG = (char) 0;
    public static final String COPY_CSV_NULL = "";
    public static final String COPY_CSV_CELL_SEP = ",";
    public static final String COPY_CSV_CELL_QUOTE = "\"";
    public static final String COPY_CSV_LINE_SEP = "\r\n";
    public static final int POOL_SIZE_MIN = 2;
    public static final int POOL_SIZE_MAX = 8;
    public static final int POOL_MAX_LIFETIME = 1000 * 60 * 15;
    public static final long MS_CANCEL_TIMEOUT = 1000 * 5;
    public static final byte JSONB_VERSION = 1;
}
