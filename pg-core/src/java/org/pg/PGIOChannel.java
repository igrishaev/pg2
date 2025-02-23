package org.pg;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channel;
import java.security.cert.Certificate;

import javax.net.ssl.SSLContext;

/*
An abstract wrapper for various IO backends such as ordinary
sockets and Unix domain sockets. Useful for experiments with
other kinds for IO backends as well (e.g. channels, etc).
 */
@SuppressWarnings("unused")
public interface PGIOChannel extends Channel, AutoCloseable {
    InputStream getInputStream();
    OutputStream getOutputStream();
    PGIOChannel upgradeToSSL(SSLContext sslContext);
    Certificate getPeerCertificate();
}
