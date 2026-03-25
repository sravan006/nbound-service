package com.rxlink.inbound.sender.orchestration;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Base64;

@Component
public class OrchestrationApplier {

    public byte[] apply(OrchestrationRule rule, byte[] payload) {
        byte[] prefix = decode(rule.getPrefixBase64());
        byte[] suffix = decode(rule.getSuffixBase64());
        if (prefix.length == 0 && suffix.length == 0) {
            return payload;
        }
        byte[] out = Arrays.copyOf(prefix, prefix.length + payload.length + suffix.length);
        System.arraycopy(payload, 0, out, prefix.length, payload.length);
        System.arraycopy(suffix, 0, out, prefix.length + payload.length, suffix.length);
        return out;
    }

    private static byte[] decode(String b64) {
        if (b64 == null || b64.isBlank()) {
            return new byte[0];
        }
        return Base64.getDecoder().decode(b64.trim());
    }
}
