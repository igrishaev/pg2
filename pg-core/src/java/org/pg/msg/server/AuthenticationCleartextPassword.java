package org.pg.msg.server;

public record AuthenticationCleartextPassword () implements IServerMessage {
    public static final int status = 3;
    public static final AuthenticationCleartextPassword INSTANCE = new AuthenticationCleartextPassword();
}
