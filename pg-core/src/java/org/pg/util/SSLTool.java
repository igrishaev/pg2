package org.pg.util;

import org.pg.error.PGError;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import java.security.cert.Certificate;

public class SSLTool {

    public static Certificate getPeerCertificate(final SSLSocket sslSocket) {
        final SSLSession session = sslSocket.getSession();
        final Certificate[] certs;
        try {
            certs = session.getPeerCertificates();
        } catch (SSLPeerUnverifiedException e) {
            throw new PGError(e, "failed to get peer certificates, socket: %s", sslSocket);
        }
        if (certs != null && certs.length > 0) {
            return certs[0];
        } else {
            throw new PGError("peer certificates are empty, socket: %s", sslSocket);
        }
    }

}
