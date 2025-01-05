package org.pg.msg.server;

import clojure.lang.IPersistentMap;
import clojure.lang.PersistentHashMap;
import org.pg.clojure.KW;
import org.pg.util.BBTool;
import org.pg.clojure.IClojure;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public record NotificationResponse(int pid,
                                   String channel,
                                   String message)
    implements IClojure, IServerMessage {

    public IPersistentMap toClojure () {
        return PersistentHashMap.create(
                KW.msg, KW.NotificationResponse,
                KW.pid, pid,
                KW.channel, channel,
                KW.message, message
        );
    }

    public static NotificationResponse fromByteBuffer (
            final ByteBuffer buf,
            final Charset charset
    ) {
        final int pid = buf.getInt();
        final String channel = BBTool.getCString(buf, charset);
        final String message = BBTool.getCString(buf, charset);
        return new NotificationResponse(pid, channel, message);
    }
}
