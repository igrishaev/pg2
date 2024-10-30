package org.pg.codec;

import clojure.lang.ITransientCollection;
import clojure.lang.PersistentVector;
import clojure.core$get_in;
import org.pg.error.PGError;
import org.pg.type.Matrix;
import org.pg.processor.IProcessor;
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
            final int oidEl,
            final CodecParams codecParams
    ) {
        final IProcessor processor = codecParams.getProcessor(oidEl);
        final int[] dims = Matrix.getDims(matrix);
        final int dimCount = dims.length;
        final int hasNulls = 1;

        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        writeInt32(out, dimCount);
        writeInt32(out, hasNulls);
        writeInt32(out, oidEl);

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
                byte[] ba = processor.encodeBin(val, codecParams).array();
                writeInt32(out, ba.length);
                out.writeBytes(ba);
            }
        }
        return ByteBuffer.wrap(out.toByteArray());
    }

    public static Object decode(
            final ByteBuffer buf,
            final CodecParams codecParams
    ) {
        final int dimCount = buf.getInt();
        buf.getInt(); // has nulls (1 or 0)
        final int elOid = buf.getInt();
        final IProcessor processor = codecParams.getProcessor(elOid);
        final int[] dims = new int[dimCount];
        for (int i = 0; i < dimCount; i++) {
            dims[i] = buf.getInt();
            buf.getInt(); // skip 4 bytes
        }

        int len;
        Object val;

        // plain array
        if (dims.length == 1) {
            ITransientCollection result = PersistentVector.EMPTY.asTransient();
            for (int i = 0; i < dims[0]; i++) {
                len = buf.getInt();
                if (len == -1) {
                    result = result.conj(null);
                } else {
                    final ByteBuffer bufEl = buf.slice();
                    bufEl.limit(len);
                    BBTool.skip(buf, len);
                    val = processor.decodeBin(bufEl, codecParams);
                    result = result.conj(val);
                }
            }
            return result.persistent();
        }

        // multi-dim array
        final int[] path = Matrix.initPath(dimCount);
        final long totalCount = Matrix.getTotalCount(dims);
        Object matrix = Matrix.create(dims);
        for (int i = 0; i < totalCount; i++) {
            Matrix.incPath(dims, path);
            len = buf.getInt();
            if (len == -1) {
                matrix = Matrix.assocIn(matrix, path, null);
            } else {
                final ByteBuffer bufEl = buf.slice();
                bufEl.limit(len);
                BBTool.skip(buf, len);
                val = processor.decodeBin(bufEl, codecParams);
                matrix = Matrix.assocIn(matrix, path, val);
            }
        }
        return matrix;
    }
}
