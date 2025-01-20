package org.pg.util;

import org.pg.error.PGError;
import org.pg.ssl.TrustManagerNoValidation;

import javax.net.ssl.*;

import java.io.IOException;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;

public class SSLTool {

    private static final String[] SSLProtocols = new String[] {
        "TLSv1.2",
        "TLSv1.1",
        "TLSv1"
    };

    public static SSLContext SSLContext(final String protocol) {
        try {
            return SSLContext.getInstance(protocol);
        } catch (NoSuchAlgorithmException e) {
            throw new PGError(e, "cannot get a SSL context, protocol: %s", protocol);
        }
    }

    public static SSLContext SSLContextDefault() {
        try {
            return SSLContext.getDefault();
        } catch (NoSuchAlgorithmException e) {
            throw new PGError(e, "cannot get a default SSL context, cause: %s", e.getMessage());
        }
    }

    public static SSLContext SSLContextNoValidation() {
        final SSLContext sslContext = SSLContext("TLS");
        try {
            sslContext.init(
                    null,
                    new TrustManager[]{TrustManagerNoValidation.INSTANCE},
                    null
            );
        } catch (KeyManagementException e) {
            throw new PGError(e, "cannot initiate ssl context: %s, cause: %s", sslContext, e.getMessage());
        }
        return sslContext;
    }

    public static Certificate getPeerCertificate(final SSLSocket sslSocket) {
        final SSLSession session = sslSocket.getSession();
        final Certificate[] certs;
        try {
            certs = session.getPeerCertificates();
        } catch (SSLPeerUnverifiedException e) {
            throw new PGError(e, "failed to get peer certificates, socket: %s, cause: %s", sslSocket, e.getMessage());
        }
        if (certs != null && certs.length > 0) {
            return certs[0];
        } else {
            throw new PGError("peer certificates are empty, socket: %s", sslSocket);
        }
    }

    public static void startHandshake(final SSLSocket socket) {
        try {
            socket.startHandshake();
        } catch (IOException e) {
            throw new PGError(e, "cannot start handshake, cause: %s", e.getMessage());
        }
    }

    public static SSLSocket connect(final SSLContext sslContext,
                                 final Socket socket,
                                 final String host,
                                 final int port,
                                 final boolean autoClose) {
        SSLSocket sslSocket = SocketTool.open(sslContext, socket, host, port, autoClose);
        sslSocket.setUseClientMode(true);
        sslSocket.setEnabledProtocols(SSLProtocols);
        startHandshake(sslSocket);
        return sslSocket;
    }

}
