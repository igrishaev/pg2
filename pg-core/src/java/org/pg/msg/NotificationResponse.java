package org.pg.msg;

import clojure.lang.IPersistentMap;
import clojure.lang.Keyword;
import clojure.lang.PersistentHashMap;
import org.pg.util.BBTool;
import org.pg.clojure.IClojure;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public record NotificationResponse(int pid,
                                   String channel,
                                   String message)
    implements IClojure {

    public IPersistentMap toClojure () {
        return PersistentHashMap.create(
                Keyword.intern("msg"), Keyword.intern("NoticeResponse"),
                Keyword.intern("pid"), pid,
                Keyword.intern("channel"), channel,
                Keyword.intern("message"), message
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
