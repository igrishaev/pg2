package org.pg.codec;

import org.pg.enums.OID;
import clojure.core$assoc_in;
import clojure.core$get_in;
import org.pg.error.PGError;
import org.pg.util.BBTool;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public final class ArrayBin {

    private static void writeInt32(final OutputStream outputStream, final int value) {
        try {
            outputStream.write(BBTool.ofInt(value).array());
        } catch (IOException e) {
            throw new PGError(e, "cannot write int32 to output stream, value: %s", value);
        }
    }

    public static ByteBuffer encode(
            final Object matrix,
            final OID oidArray,
            final CodecParams codecParams
    ) {
        final int[] dims = Matrix.getDims(matrix);
        final int dimCount = dims.length;
        final int hasNulls = 1;

        final OID oidEl = OID.INT4;

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        writeInt32(out, dimCount);
        writeInt32(out, hasNulls);
        writeInt32(out, oidEl.toInt());

        for (int dim: dims) {
            writeInt32(out, dim);
            writeInt32(out, 1);
        }

        final long totalCount = Matrix.getTotalCount(dims);
        final int[] path = Matrix.initPath(dimCount);

        for (int i = 0; i < totalCount; i++) {
            Matrix.incPath(dims, path);
            Object val = core$get_in.invokeStatic(matrix, path);
            if (val == null) {
                writeInt32(out, -1);
            } else {
                byte[] ba = EncoderBin.encode(val, oidEl, codecParams).array();
                writeInt32(out, ba.length);
                out.writeBytes(ba);
            }
        }
        return ByteBuffer.wrap(out.toByteArray());
    }

    public static Object decode(
            final ByteBuffer buf,
            final OID arrayOid,
            final CodecParams codecParams
    ) {
        final int dimCount = buf.getInt();
        buf.getInt(); // has nulls (1 or 0)
        final OID elOid = OID.ofInt(buf.getInt());
        final int[] dims = new int[dimCount];
        for (int i = 0; i < dimCount; i++) {
            dims[i] = buf.getInt();
            buf.getInt(); // skip 4 bytes
        }
        final int[] path = Matrix.initPath(dimCount);
        int len;
        Object val;
        final long totalCount = Matrix.getTotalCount(dims);
        Object matrix = Matrix.create(dims);
        for (int i = 0; i < totalCount; i++) {
            Matrix.incPath(dims, path);
            len = buf.getInt();
            if (len != -1) {
                val = DecoderBin.decode(buf, elOid, codecParams);
                matrix = core$assoc_in.invokeStatic(matrix, path, val);
            }
        }
        return matrix;
    }
}
