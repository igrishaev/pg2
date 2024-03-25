package org.pg.codec;

import org.pg.enums.OID;
import org.pg.util.ArrTool;

import java.nio.ByteBuffer;

public final class ArrayBin {

    public static Object decode(
            final ByteBuffer buf,
            final OID arrayOid,
            final CodecParams codecParams
    ) {
        final int dimCount = buf.getInt();
        buf.getInt(); // has nulls (1 or 0)
        final OID elOid = OID.ofInt(buf.getInt());
        final int[] dims = new int[dimCount];
        final int[] path = new int[dimCount];
        int dim;
        int len;
        Object val;
        long totalCount = 1;
        for (int i = 0; i < dimCount; i++) {
            dim = buf.getInt();
            dims[i] = dim;
            totalCount *= dim;
            buf.getInt(); // skip 4 bytes
        }
        if (dims.length == 0) {
            totalCount = 0;
        }
        final Object array = ArrTool.create(dims);
        for (int i = 0; i < totalCount; i++) {
            len = buf.getInt();
            if (len != -1) {
                val = DecoderBin.decode(buf, elOid, codecParams);
                ArrTool.setVal(array, val, path);
            }
            ArrTool.incPath(dims, path);
        }
        return array;
    }
}
