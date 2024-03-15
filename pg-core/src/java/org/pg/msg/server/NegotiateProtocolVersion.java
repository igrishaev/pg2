package org.pg.msg.server;

import clojure.lang.IPersistentMap;
import clojure.lang.Keyword;
import clojure.lang.PersistentHashMap;
import clojure.lang.PersistentVector;
import org.pg.util.BBTool;
import org.pg.clojure.IClojure;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

public record NegotiateProtocolVersion(
        int version,
        int paramCount,
        String[] params
) implements IClojure, IServerMessage {

    public IPersistentMap toClojure () {
        return PersistentHashMap.create(
                Keyword.intern("msg"), Keyword.intern("NegotiateProtocolVersion"),
                Keyword.intern("version"), version,
                Keyword.intern("param-count"), paramCount,
                Keyword.intern("params"), PersistentVector.create(Arrays.asList(params))
        );
    }

    public static NegotiateProtocolVersion fromByteBuffer(
            final ByteBuffer buf,
            final Charset charset
    ) {
        final int version = buf.getInt();
        final int paramCount = buf.getInt();
        final String[] params = new String[paramCount];
        for (int i = 0; i < paramCount; i++) {
            params[i] = BBTool.getCString(buf, charset);
        }
        return new NegotiateProtocolVersion(version, paramCount, params);
    }
}
