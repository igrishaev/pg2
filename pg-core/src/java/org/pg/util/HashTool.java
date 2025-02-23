package org.pg.util;

import org.pg.error.PGError;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public final class HashTool {

    public static MessageDigest getDigest (final String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new PGError(e, "cannot find %s message digest", algorithm);
        }
    }

    public static Mac getMac (final String algorithm) {
        try {
            return Mac.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new PGError(e, "cannot find %s Mac instance", algorithm);
        }
    }

    public static byte[] MD5encode(byte[] input) {
        final MessageDigest md5 = getDigest("MD5");
        md5.update(input);
        return md5.digest();
    }

    public static byte[] Sha256 (final byte[] input) {
        final MessageDigest sha = getDigest("SHA-256");
        sha.update(input);
        return sha.digest();
    }

    public static byte[] base64decode (final byte[] input) {
        return Base64.getDecoder().decode(input);
    }

    public static byte[] base64encode (final byte[] input) {
        return Base64.getEncoder().encode(input);
    }

    public static byte[] HmacSha256 (final byte[] secret, final byte[] message) {
        final Mac mac = getMac("HmacSHA256");
        final SecretKeySpec sks = new SecretKeySpec(secret, "HmacSHA256");
        try {
            mac.init(sks);
        } catch (InvalidKeyException e) {
            throw new PGError(e, "cannot initiate MAC with a key");
        }
        return mac.doFinal(message);
    }

}
