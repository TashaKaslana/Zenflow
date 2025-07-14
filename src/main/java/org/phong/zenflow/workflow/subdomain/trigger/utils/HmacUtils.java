package org.phong.zenflow.workflow.subdomain.trigger.utils;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

public class HmacUtils {

    public static void verifySignatureOrThrow(String secret, String rawBody, String providedSignature) {
        if (providedSignature == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing signature");
        }

        String expectedSignature = computeHmac(secret, rawBody);
        if (!MessageDigest.isEqual(expectedSignature.getBytes(), providedSignature.getBytes())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid signature");
        }
    }

    public static String computeHmac(String secret, String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] rawHmac = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("Error computing HMAC", e);
        }
    }
}
