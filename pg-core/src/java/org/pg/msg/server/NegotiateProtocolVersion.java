package org.pg.msg.server;

import clojure.lang.IPersistentMap;
import clojure.lang.PersistentHashMap;
import clojure.lang.PersistentVector;
import org.pg.clojure.KW;
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

    @Override
    public String toString() {
        return String.format("NegotiateProtocolVersion[version=%s, paramCount=%s, params=%s]",
                version,
                paramCount,
                Arrays.toString(params)
        );
    }

    public IPersistentMap toClojure () {
        return PersistentHashMap.create(
                KW.msg, KW.NegotiateProtocolVersion,
                KW.version, version,
                KW.paramCount, paramCount,
                KW.params, PersistentVector.create(Arrays.asList(params))
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
