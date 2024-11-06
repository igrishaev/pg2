package org.pg.auth;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.pg.error.PGError;
import org.pg.util.ByteTool;
import org.pg.util.HashTool;
import org.pg.util.NormTool;

public final class ScramSha256 {

    public enum BindFlag {
        NO,
        REQUIRED;
        @Override
        public String toString() {
            return switch (this) {
                case NO -> "n";
                case REQUIRED -> "p";
            };
        }
    }

    public enum BindType {
        NONE,
        TLS_SERVER_END_POINT;
        @Override
        public String toString() {
            return switch (this) {
                case NONE -> "";
                case TLS_SERVER_END_POINT -> "tls-server-end-point";
            };
        }
    }

    public static byte[] Hi(
            final byte[] secret,
            final byte[] message,
            final int iterations
    ) {
        byte[] u = new byte[32];
        byte[] uNext;
        byte[] msg = ByteTool.concat(message, new byte[] {0, 0, 0, 1});

        for (int i = 0; i < iterations; i++) {
            uNext = HashTool.HmacSha256(secret, msg);
            msg = uNext;
            u = ByteTool.xor(u, uNext);
        }

        return u;
    }

    public static byte[] H(byte[] input) {
        return HashTool.Sha256(input);
    }

    public static Map<String, String> parseMessage(final String message) {
        final Map<String, String> result = new HashMap<>();
        final String[] pairs = message.split(",");
        for (final String pair: pairs) {
            final String[] kv = pair.split("=", 2);
            final String k = kv[0].strip();
            final String v = kv[1].strip();
            result.put(k, v);
        }
        return result;
    }

    public record Step1 (
            String user,
            String password,
            String gs2header,
            String clientFirstMessageBare,
            String clientFirstMessage,
            byte[] bindData
    ) {}

    public static Step1 step1_clientFirstMessage (
            final String user,
            final String password,
            final BindFlag bindFlag,
            final BindType bindType,
            final byte[] bindData
    ) {
        final String gs2header = switch (bindFlag) {
            case NO -> bindFlag.toString();
            case REQUIRED -> bindFlag + "=" + bindType;
        };
        final String nonce = UUID.randomUUID().toString();
        final String clientFirstMessageBare = "n=" + user + ",r=" + nonce;
        final String clientFirstMessage = gs2header + ",," +clientFirstMessageBare;
        return new Step1(
                user,
                password,
                gs2header,
                clientFirstMessageBare,
                clientFirstMessage,
                bindData
        );
    }

    public record Step2 (
            byte[] salt,
            String nonce,
            int iterationCount,
            String serverFirstMessage
    ) {}

    public static String getField (final Map<String, String> keyval, final String key) {
        final String val = keyval.get(key);
        if (val == null) {
            throw new PGError("the '%s' field is missing", key);
        }
        else {
            return val;
        }
    }

    public static Step2 step2_serverFirstMessage (final String serverFirstMessage) {
        final Map<String, String> keyval = parseMessage(serverFirstMessage);
        final String saltEncoded = getField(keyval, "s");
        final byte[] salt = HashTool.base64decode(saltEncoded.getBytes(StandardCharsets.UTF_8));
        final String nonce = getField(keyval, "r");
        final int iterationCount = Integer.parseInt(getField(keyval, "i"));
        return new Step2(
                salt,
                nonce,
                iterationCount,
                serverFirstMessage
        );
    }

    public record Step3 (
            byte[] ServerSignature,
            String clientFinalMessage
    ) {}

    static final byte[] commas = new byte[] {',', ','};

    public static Step3 step3_clientFinalMessage (final Step1 step1, final Step2 step2) {
        final byte[] bindArray = ByteTool.concat(
                step1.gs2header().getBytes(StandardCharsets.UTF_8),
                commas,
                step1.bindData()
        );
        final String channelBinding = new String(HashTool.base64encode(bindArray), StandardCharsets.UTF_8);
        final String nonce = step2.nonce;
        final String clientFinalMessageWithoutProof = "c=" + channelBinding + ",r=" + nonce;
        final String AuthMessage =
                step1.clientFirstMessageBare
                + "," + step2.serverFirstMessage
                + "," + clientFinalMessageWithoutProof;
        final byte[] passwordNorm = NormTool.normalizeNfc(step1.password).getBytes(StandardCharsets.UTF_8);
        final byte[] SaltedPassword = Hi(passwordNorm, step2.salt, step2.iterationCount);
        final byte[] ClientKey = HashTool.HmacSha256(SaltedPassword, "Client Key".getBytes(StandardCharsets.UTF_8));
        final byte[] StoredKey = H(ClientKey);
        final byte[] ClientSignature = HashTool.HmacSha256(StoredKey, AuthMessage.getBytes(StandardCharsets.UTF_8));
        final byte[] ClientProof = ByteTool.xor(ClientKey, ClientSignature);
        final byte[] ServerKey = HashTool.HmacSha256(SaltedPassword, "Server Key".getBytes(StandardCharsets.UTF_8));
        final byte[] ServerSignature = HashTool.HmacSha256(ServerKey, AuthMessage.getBytes(StandardCharsets.UTF_8));
        final String proof = new String(HashTool.base64encode(ClientProof), StandardCharsets.UTF_8);
        final String clientFinalMessage = clientFinalMessageWithoutProof + ",p=" + proof;
        return new Step3(ServerSignature, clientFinalMessage);

    }

    public record Step4 (
            byte[] ServerSignature2,
            String serverFinalMessage
    ) {}

    public static Step4 step4_serverFinalMessage (final String serverFinalMessage) {
        final Map<String, String> keyval = parseMessage(serverFinalMessage);
        final String verifier = getField(keyval, "v");
        final byte[] ServerSignature2 = HashTool.base64decode(verifier.getBytes(StandardCharsets.UTF_8));
        return new Step4(ServerSignature2, serverFinalMessage);
    }

    public static void step5_verifyServerSignature (final Step3 step3, final Step4 step4) {
        if (!Arrays.equals(step3.ServerSignature(), step4.ServerSignature2())) {
            throw new PGError("server signatures do not match!");
        }
    }


    public static Pipeline pipeline () {
        return new Pipeline();
    }

    public static class Pipeline {
        public Step1 step1;
        public Step2 step2;
        public Step3 step3;
        public Step4 step4;
    }

}
