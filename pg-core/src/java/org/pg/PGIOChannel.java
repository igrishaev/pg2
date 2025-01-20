package org.pg;

import java.io.Closeable;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channel;
import java.security.cert.Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;

@SuppressWarnings("unused")
public interface PGIOChannel extends Channel, Closeable {
    public InputStream getInputStream();
    public OutputStream getOutputStream();
    public PGIOChannel upgradeToSSL(SSLContext sslContext);
    public Certificate getPeerCertificate();
}
