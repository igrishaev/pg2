package org.pg;

import org.pg.util.BBTool;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;


public final class Payload {

    private Integer size;
    private final ArrayList<Object> items;

    public Payload() {
        size = 0;
        items = new ArrayList<>();
    }

    public Payload addInteger(final Integer i) {
        size += 4;
        items.add(i);
        return this;
    }

    public Payload addShort(final Short s) {
        size += 2;
        items.add(s);
        return this;
    }

    public Payload addByte(final Byte b) {
        size += 1;
        items.add(b);
        return this;
    }

    public Payload addBytes(final byte[] buf) {
        size += buf.length;
        items.add(buf);
        return this;
    }

    public Payload addUnsignedShort (final Integer i) {
        size += 2;
        final ByteBuffer buf = ByteBuffer.allocate(4);
        buf.putInt(i);
        BBTool.skip(buf, -2);
        items.add(buf);
        return this;
    }

    public Payload addUnsignedInteger (final Long l) {
        size += 4;
        final ByteBuffer buf = ByteBuffer.allocate(8);
        buf.putLong(l);
        BBTool.skip(buf, -4);
        items.add(buf);
        return this;
    }

    public Payload addCString(final String s) {
        return addCString(s, StandardCharsets.UTF_8);
    }

    public Payload addCString(final String s, final Charset charset) {
        final byte[] bytes = s.getBytes(charset);
        size = size + bytes.length + 1;
        items.add(bytes);
        items.add((byte)0);
        return this;
    }

    public ByteBuffer toByteBuffer() {
        return toByteBuffer(Const.NULL_TAG);
    }

    public ByteBuffer toByteBuffer(char tag) {

        ByteBuffer buf;

        if (tag == Const.NULL_TAG) {
            buf = ByteBuffer.allocate(size + 4);
        } else {
            buf = ByteBuffer.allocate(size + 5);
            buf.put((byte)tag);
        }

        buf.putInt(size + 4);

        for(Object x: items) {
            switch (x.getClass().getSimpleName()) {
                case "Integer" -> buf.putInt((int)x);
                case "Short" -> buf.putShort((short)x);
                case "ByteBuffer", "HeapByteBuffer" -> buf.put((ByteBuffer)x);
                case "Byte" -> buf.put((byte)x);
                case "Long" -> buf.putLong((long)x);
                case "byte[]" -> buf.put((byte[])x);
                default -> throw new PGError("unsupported item: %s, type: %s", x, x.getClass());
            }
        }
        return buf;
    }
}
