package org.pg.processor;

import clojure.lang.*;
import org.pg.codec.CodecParams;
import org.pg.error.PGError;
import org.pg.util.BBTool;
import org.pg.util.TypeTool;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

public class Hstore extends AProcessor {

    private static ByteBuffer encodeMapBin(final Map<?,?> map, final CodecParams codecParams) throws IOException {
        final Charset charset = codecParams.clientCharset();
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final DataOutputStream dos = new DataOutputStream(baos);
        final int total = map.size();
        dos.writeInt(total);
        Object keyObj;
        Object valObj;
        String key;
        String val;
        byte[] buf;
        for (Map.Entry<?, ?> me: map.entrySet()) {

            keyObj = me.getKey();
            if (keyObj == null) {
                key = "";
            } else if (keyObj instanceof Keyword kw) {
                key = kw.toString().substring(1);
            } else if (keyObj instanceof String s) {
                key = s;
            } else {
                key = keyObj.toString();
            }
            buf = key.getBytes(charset);
            dos.writeInt(buf.length);
            dos.write(buf);

            valObj = me.getValue();
            if (valObj == null) {
                dos.writeInt(-1);
                continue;
            } else if (valObj instanceof String s) {
                val = s;
            } else if (valObj instanceof Keyword kw) {
                val = kw.toString().substring(1);
            } else {
                val = valObj.toString();
            }
            buf = val.getBytes(charset);
            dos.writeInt(buf.length);
            dos.write(buf);
        }
        dos.close();
        baos.close();
        return ByteBuffer.wrap(baos.toByteArray());
    }

    @Override
    public ByteBuffer encodeBin(final Object x, final CodecParams codecParams) {
        if (x instanceof Map<?,?> map) {
            try {
                return encodeMapBin(map, codecParams);
            } catch (IOException e) {
                throw new PGError(e, "cannot binary encode hstore: %s", TypeTool.repr(x));
            }
        } else {
            return binEncodingError(x);
        }
    }

    @Override
    public Object decodeBin(final ByteBuffer bb, final CodecParams codecParams) {
        final int total = bb.getInt();
        ITransientMap result = PersistentHashMap.EMPTY.asTransient();
        String key;
        String val;
        int len;
        for (int i = 0; i < total; i++) {
            len = bb.getInt();
            key = BBTool.getString(bb, len, codecParams.serverCharset());
            len = bb.getInt();
            if (len == -1) {
                val = null;
            } else {
                val = BBTool.getString(bb, len, codecParams.serverCharset());
            }
            result = result.assoc(Keyword.intern(key), val);
        }
        return result.persistent();
    }

}
