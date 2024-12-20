package org.pg.util;

import org.pg.error.PGError;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import java.security.cert.Certificate;

public class SSLTool {

    public static Certificate getPeerCertificate(final SSLSession session) {
        final Certificate[] certs;
        try {
            certs = session.getPeerCertificates();
        } catch (SSLPeerUnverifiedException e) {
            throw new PGError(e, "failed to get peer certificates, session: %s", session);
        }
        if (certs != null && certs.length > 0) {
            return certs[0];
        } else {
            throw new PGError("peer certificates are empty, session: %s", session);
        }
    }

    public static Certificate getPeerCertificate(final SSLEngine engine) {
        return getPeerCertificate(engine.getSession());
    }

    @SuppressWarnings("unused")
    public static Certificate getPeerCertificate(final SSLSocket socket) {
        return getPeerCertificate(socket.getSession());
    }

}
