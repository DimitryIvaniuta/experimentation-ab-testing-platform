package com.github.dimitryivaniuta.experimentation.service;

import com.github.dimitryivaniuta.experimentation.domain.ExperimentDefinition;
import com.github.dimitryivaniuta.experimentation.domain.VariantRecord;
import com.github.dimitryivaniuta.experimentation.exception.BusinessRuleException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

/**
 * Selects variants by deterministic hashing of experiment key, salt, and privacy-safe user hash.
 *
 * <p>The implementation deliberately separates traffic allocation from variant selection. This prevents a common
 * A/B testing bug where a 10% rollout accidentally sends all included users to the first low bucket of variant
 * weights instead of first deciding whether the user is in the rollout and then selecting among all variants.</p>
 */
@Component
public class DeterministicVariantAssigner {

    private static final int FULL_TRAFFIC_BASIS_POINTS = 10_000;
    private static final String TRAFFIC_BUCKET_PURPOSE = "traffic";
    private static final String VARIANT_BUCKET_PURPOSE = "variant";

    /**
     * Returns whether a user hash is inside the configured traffic allocation.
     *
     * <p>The result is stable for the same experiment salt and user hash. Existing persisted assignments should be
     * honored before this method is checked, so previously assigned users do not churn when traffic is reduced.</p>
     *
     * @param definition experiment definition
     * @param userHash privacy-safe HMAC user hash
     * @return {@code true} when the user is inside the rollout bucket
     */
    public boolean isInTraffic(final ExperimentDefinition definition, final String userHash) {
        int allocation = definition.experiment().trafficAllocationBp();
        if (allocation >= FULL_TRAFFIC_BASIS_POINTS) {
            return true;
        }
        if (allocation <= 0) {
            return false;
        }
        int bucket = positiveBucket(definition, userHash, TRAFFIC_BUCKET_PURPOSE, FULL_TRAFFIC_BASIS_POINTS);
        return bucket < allocation;
    }

    /**
     * Assigns a user hash to one weighted variant.
     *
     * <p>Call {@link #isInTraffic(ExperimentDefinition, String)} first for users without an existing persisted
     * assignment. This method only chooses a variant after traffic eligibility has been decided.</p>
     *
     * @param definition experiment definition with ordered weighted variants
     * @param userHash privacy-safe user hash
     * @return selected variant
     */
    public VariantRecord assign(final ExperimentDefinition definition, final String userHash) {
        int totalWeight = definition.variants().stream().mapToInt(VariantRecord::weight).sum();
        if (totalWeight <= 0) {
            throw new BusinessRuleException("Experiment must have positive variant weights");
        }
        int bucket = positiveBucket(definition, userHash, VARIANT_BUCKET_PURPOSE, totalWeight);
        int cumulative = 0;
        for (VariantRecord variant : definition.variants()) {
            cumulative += variant.weight();
            if (bucket < cumulative) {
                return variant;
            }
        }
        return definition.variants().getLast();
    }

    private int positiveBucket(final ExperimentDefinition definition, final String userHash,
                               final String purpose, final int modulo) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String input = definition.experiment().key() + ':'
                    + definition.experiment().salt() + ':'
                    + purpose + ':'
                    + userHash;
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            String hex = HexFormat.of().formatHex(bytes);
            return new BigInteger(hex, 16).mod(BigInteger.valueOf(modulo)).intValue();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to calculate deterministic assignment bucket", exception);
        }
    }
}
