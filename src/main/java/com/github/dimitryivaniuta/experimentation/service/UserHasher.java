package com.github.dimitryivaniuta.experimentation.service;

import com.github.dimitryivaniuta.experimentation.config.ExperimentationProperties;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Hashes raw user identifiers with HMAC-SHA256 so the platform never stores raw user ids.
 */
@Component
@RequiredArgsConstructor
public class UserHasher {

    private static final String HMAC_SHA_256 = "HmacSHA256";
    private final ExperimentationProperties properties;

    /**
     * Converts a raw user id to a deterministic privacy-safe hex hash.
     *
     * @param rawUserId raw caller-provided user identifier
     * @return lowercase HMAC-SHA256 hex hash
     */
    public String hash(final String rawUserId) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA_256);
            SecretKeySpec key = new SecretKeySpec(
                    properties.privacy().hmacSecret().getBytes(StandardCharsets.UTF_8),
                    HMAC_SHA_256
            );
            mac.init(key);
            return HexFormat.of().formatHex(mac.doFinal(rawUserId.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash user id", exception);
        }
    }
}
