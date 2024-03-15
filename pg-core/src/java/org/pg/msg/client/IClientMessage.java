package org.pg.msg.client;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public interface IClientMessage {
    ByteBuffer encode(Charset encoding);
}
