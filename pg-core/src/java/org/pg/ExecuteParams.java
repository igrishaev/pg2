package org.pg;

import clojure.lang.IFn;
import clojure.lang.Named;
import clojure.lang.RT;
import org.pg.clojure.CljAPI;
import org.pg.codec.CodecParams;
import org.pg.enums.CopyFormat;
import org.pg.enums.OID;
import org.pg.error.PGError;
import org.pg.reducer.*;
import org.pg.util.TypeTool;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

public record ExecuteParams (
        List<Object> params,
        List<Object> objOids,
        IFn reducer,
        long maxRows,
        IFn fnKeyTransform,
        OutputStream outputStream,
        InputStream inputStream,
        boolean binaryEncode,
        boolean binaryDecode,
        String CSVNull,
        String CSVCellSep,
        String CSVQuote,
        String CSVLineSep,
        CopyFormat copyFormat,
        int copyBufSize,
        List<List<Object>> copyInRows,
        boolean isCopyInRows,
        List<Map<Object, Object>> copyInMaps,
        boolean isCopyInMaps,
        List<Object> copyInKeys
) {

    public static Builder builder() {
        return new Builder();
    }

    public static ExecuteParams INSTANCE = standard();

    public static ExecuteParams standard () {
        return new Builder().build();
    }

    /*
    Turn object OIDs of mixed types into an array of integer OIDs.
    The CodecParams value serves as a source of Postgres types.
     */
    public int[] getIntOids (final CodecParams codecParams) {
        final int len = objOids.size();
        final int[] result = new int[len];
        int i = -1;
        int oidInt;
        String typeName;
        String namespace;
        for (Object objOid: objOids) {
            i++;
            if (objOid == null) {
                result[i] = OID.DEFAULT;
            } else if (objOid instanceof Number) {
                result[i] = RT.intCast(objOid);
            } else if (objOid instanceof String s) {
                oidInt = codecParams.typeToOid(s);
                result[i] = oidInt;
            } else if (objOid instanceof Named nm) {
                namespace = nm.getNamespace();
                if (namespace == null) {
                    namespace = "public";
                }
                typeName = namespace + "." + nm.getName();
                oidInt = codecParams.typeToOid(typeName);
                result[i] = oidInt;
            } else {
                throw new PGError("wrong OID: %s", TypeTool.repr(objOid));
            }
        }
        return result;
    }

    public final static class Builder {

        private List<Object> params = Collections.emptyList();
        private List<Object> objOids = Collections.emptyList();
        private IFn reducer = Default.INSTANCE;
        private long maxRows = 0;
        private IFn fnKeyTransform = CljAPI.keyword;
        private OutputStream outputStream = OutputStream.nullOutputStream();
        private InputStream inputStream = InputStream.nullInputStream();
        private boolean binaryEncode = false;
        private boolean binaryDecode = false;
        private String CSVNull = Const.COPY_CSV_NULL;
        private String CSVCellSep = Const.COPY_CSV_CELL_SEP;
        private String CSVQuote = Const.COPY_CSV_CELL_QUOTE;
        private String CSVLineSep = Const.COPY_CSV_LINE_SEP;
        private CopyFormat copyFormat = CopyFormat.CSV;
        private int copyBufSize = Const.COPY_BUFFER_SIZE;
        private List<List<Object>> copyInRows = Collections.emptyList();
        private boolean isCopyInRows = false;
        private List<Map<Object, Object>> copyInMaps = Collections.emptyList();
        private boolean isCopyInMaps = false;
        List<Object> copyInKeys = Collections.emptyList();

        public Builder params (final List<Object> params) {
            this.params = Objects.requireNonNull(params);
            return this;
        }

        @SuppressWarnings("unused")
        public Builder copyInRows (final List<List<Object>> copyInRows) {
            this.isCopyInRows = true;
            this.copyInRows = Objects.requireNonNull(copyInRows, "COPY IN rows cannot be null");
            return this;
        }

        @SuppressWarnings("unused")
        public Builder copyInMaps (final List<Map<Object, Object>> copyInMaps) {
            this.isCopyInMaps = true;
            this.copyInMaps = Objects.requireNonNull(copyInMaps, "COPY IN maps cannot be null");
            return this;
        }

        @SuppressWarnings("unused")
        public Builder copyInKeys (final List<Object> copyInKeys) {
            this.copyInKeys = Objects.requireNonNull(copyInKeys, "COPY IN map keys cannot be null");
            return this;
        }

        @SuppressWarnings("unused")
        public Builder params (final Object[] params) {
            this.params = Arrays.asList(params);
            return this;
        }

        @SuppressWarnings("unused")
        public Builder outputStream (final OutputStream outputStream) {
            this.outputStream = Objects.requireNonNull(outputStream, "the output stream cannot be null");
            return this;
        }

        @SuppressWarnings("unused")
        public Builder inputStream (final InputStream inputStream) {
            this.inputStream = Objects.requireNonNull(inputStream, "the input stream cannot be null");
            return this;
        }

        @SuppressWarnings("unused")
        public Builder fnKeyTransform (final IFn fnKeyTransform) {
            this.fnKeyTransform = Objects.requireNonNull(fnKeyTransform);
            return this;
        }

        public Builder OIDs (final List<Object> OIDs) {
            if (OIDs == null) {
                return this;
            }
            this.objOids = OIDs;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder reducer (final IFn reducer) {
            this.reducer = Objects.requireNonNull(reducer);
            return this;
        }

        @SuppressWarnings("unused")
        public Builder maxRows (final long maxRows) {
            this.maxRows = maxRows;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder binaryEncode (final boolean binaryEncode) {
            this.binaryEncode = binaryEncode;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder binaryDecode (final boolean binaryDecode) {
            this.binaryDecode = binaryDecode;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder CSVNull (final String CSVNull) {
            this.CSVNull = Objects.requireNonNull(CSVNull);
            return this;
        }

        @SuppressWarnings("unused")
        public Builder CSVCellSep (final String CSVCellSep) {
            this.CSVCellSep = Objects.requireNonNull(CSVCellSep);
            return this;
        }

        @SuppressWarnings("unused")
        public Builder CSVQuote (final String CSVQuote) {
            this.CSVQuote = Objects.requireNonNull(CSVQuote);
            return this;
        }

        @SuppressWarnings("unused")
        public Builder CSVLineSep (final String CSVLineSep) {
            this.CSVLineSep = Objects.requireNonNull(CSVLineSep);
            return this;
        }

        @SuppressWarnings("unused")
        public Builder copyFormat (final CopyFormat format) {
            this.copyFormat = Objects.requireNonNull(format);
            return this;
        }

        @SuppressWarnings("unused")
        public Builder setCSV () {
            this.copyFormat = CopyFormat.CSV;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder setBin () {
            this.copyFormat = CopyFormat.BIN;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder setTab () {
            this.copyFormat = CopyFormat.TAB;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder copyBufSize (final int bufSize) {
            this.copyBufSize = bufSize;
            return this;
        }

        public ExecuteParams build () {
            return new ExecuteParams(
                    params,
                    objOids,
                    reducer,
                    maxRows,
                    fnKeyTransform,
                    outputStream,
                    inputStream,
                    binaryEncode,
                    binaryDecode,
                    CSVNull,
                    CSVCellSep,
                    CSVQuote,
                    CSVLineSep,
                    copyFormat,
                    copyBufSize,
                    copyInRows,
                    isCopyInRows,
                    copyInMaps,
                    isCopyInMaps,
                    copyInKeys
            );
        }
    }
}
