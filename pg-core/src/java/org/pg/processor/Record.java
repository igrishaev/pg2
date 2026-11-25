package org.pg.processor;

import org.pg.codec.CodecParams;
import org.pg.util.BBTool;

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
        int len;
        IProcessor processor;
        ByteBuffer bbEl;
        List<Object> result = new ArrayList<>(count);
        Object value;
        for (int i = 0; i < count; i++) {
            oid = bb.getInt();
            len = bb.getInt();
            bbEl = BBTool.slice(bb, len);
            processor = codecParams.getProcessor(oid);
            value = processor.decodeBin(bbEl, codecParams);
            BBTool.skip(bb, len);
            result.add(value);
        }
        return result;
    }

    @Override
    public List<Object> decodeTxt(final String string, final CodecParams codecParams) {
        throw new RuntimeException("decodeTxt not implemented");
    }
}
