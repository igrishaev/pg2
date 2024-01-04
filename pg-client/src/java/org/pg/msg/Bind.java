package org.pg.msg;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.pg.enums.Format;
import org.pg.Payload;
import org.pg.util.BBTool;

public record Bind (

        String portal,
        String statement,
        byte[][] values,
        Format paramsFormat,
        Format columnFormat

) implements IMessage {

    public byte[][] toByteArrays () {

        int arrayCount = 2;
        int payloadLen = 0;

        for (byte[] value : values) {
            if (value == null) {
                arrayCount += 1;
                payloadLen += 4;
            }
            else {
                arrayCount += 2;
                payloadLen += 4 + value.length;
            }
        }

        final int leadSize = 1 + 4 + portal.length() + 1 + statement.length() + 1 + 2 + 2 + 2;
        final ByteBuffer bbLead = ByteBuffer.allocate(leadSize);
        bbLead.put((byte)'B');
        bbLead.putInt(leadSize - 1 + payloadLen + 4);
        bbLead.put(portal.getBytes(StandardCharsets.UTF_8));
        bbLead.put((byte)0);
        bbLead.put(statement.getBytes(StandardCharsets.UTF_8));
        bbLead.put((byte)0);
        bbLead.putShort((short)1);
        bbLead.putShort(paramsFormat().toCode());
        bbLead.putShort((short)values.length);

        final byte[][] arrays = new byte[arrayCount][];
        arrays[0] = bbLead.array();
        int i = 1;

        for (byte[] value : values) {
            if (value == null) {
                arrays[i] = BBTool.ofInt(-1).array();
            }
            else {
                arrays[i] = BBTool.ofInt(value.length).array();
                i++;
                arrays[i] = value;
            }
            i++;
        }

        final ByteBuffer bbTail = ByteBuffer.allocate(4);
        bbTail.putShort((short)1);
        bbTail.putShort(columnFormat.toCode());

        arrays[i] = bbTail.array();

        return arrays;
    }

    public ByteBuffer encode(final Charset charset) {
        final Payload payload = new Payload()
                .addCString(portal)
                .addCString(statement)
                .addShort((short)1)
                .addShort(paramsFormat.toCode())
                .addUnsignedShort(values.length);

        for (byte[] bytes: values) {
            if (bytes == null) {
                payload.addInteger(-1);
            }
            else {
                payload.addInteger(bytes.length);
                payload.addBytes(bytes);
            }
        }

        payload.addShort((short)1);
        payload.addShort(columnFormat.toCode());

        return payload.toByteBuffer('B');
    }
}
