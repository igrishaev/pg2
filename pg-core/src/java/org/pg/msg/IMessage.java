package org.pg.msg;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public interface IMessage {
    ByteBuffer encode(Charset encoding);
}
