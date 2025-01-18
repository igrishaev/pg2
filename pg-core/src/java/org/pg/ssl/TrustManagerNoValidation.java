package org.pg.ssl;

import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

public class TrustManagerNoValidation implements X509TrustManager {

    public final static TrustManagerNoValidation INSTANCE = new TrustManagerNoValidation();

    private TrustManagerNoValidation() {}

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }

    @Override
    public void checkClientTrusted(final X509Certificate[] certs, final String authType) {
    }

    @Override
    public void checkServerTrusted(final X509Certificate[] certs, final String authType) {
    }

}

