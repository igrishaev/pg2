package org.pg.msg.server;

public record PortalSuspended() implements IServerMessage {
    public static PortalSuspended INSTANCE = new PortalSuspended();
}
