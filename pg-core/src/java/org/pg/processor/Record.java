package org.pg.processor;

import org.pg.codec.CodecParams;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class Record extends AProcessor {

    @Override
    public ByteBuffer encodeBin(final Object x, final CodecParams codecParams) {
        throw new RuntimeException("encodeBin not implemented");
    }

    @Override
    public String encodeTxt(final Object x, final CodecParams codecParams) {
        throw new RuntimeException("encodeTxt not implemented");
    }

    @Override
    public List<Object> decodeBin(final ByteBuffer bb, final CodecParams codecParams) {
        int count = bb.getInt();
        int oid;
        IProcessor processor;
        List<Object> result = new ArrayList<>(count);
        Object value;
        for (int i = 0; i < count; i++) {
            oid = bb.getInt();
            bb.getInt(); // len
            processor = Processors.getProcessor(oid);
            value = processor.decodeBin(bb.slice(), codecParams);
            result.add(value);
        }
        return result;
    }

    @Override
    public List<Object> decodeTxt(final String string, final CodecParams codecParams) {
        throw new RuntimeException("decodeTxt not implemented");
    }
}
