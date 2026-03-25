package com.rxlink.inbound.router.routing;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class RoutingKeyResolver {

    public String resolve(byte[] payload, String explicitKey) {
        if (explicitKey != null && !explicitKey.isBlank()) {
            return explicitKey.trim();
        }
        String hl7Key = tryHl7SendingApplication(payload);
        if (hl7Key != null) {
            return hl7Key;
        }
        return "hash:" + sha256Prefix(payload);
    }

    private static String tryHl7SendingApplication(byte[] payload) {
        if (payload == null || payload.length < 8) {
            return null;
        }
        String head = new String(payload, 0, Math.min(payload.length, 4096), StandardCharsets.ISO_8859_1);
        if (!head.startsWith("MSH|")) {
            return null;
        }
        int lineEnd = head.indexOf('\r');
        if (lineEnd < 0) {
            lineEnd = head.indexOf('\n');
        }
        if (lineEnd < 0) {
            lineEnd = head.length();
        }
        String msh = head.substring(0, lineEnd);
        String[] fields = msh.split("\\|", 10);
        if (fields.length > 3 && !fields[3].isBlank()) {
            return fields[3].trim();
        }
        return null;
    }

    private static String sha256Prefix(byte[] payload) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            int len = Math.min(payload.length, 4096);
            md.update(payload, 0, len);
            byte[] digest = md.digest();
            return HexFormat.of().formatHex(digest, 0, 8);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
