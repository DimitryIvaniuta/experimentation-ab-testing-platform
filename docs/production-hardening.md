# Production Hardening Notes

## Updated implementation focus

This iteration keeps the platform simple but fixes and improves the parts that matter most in real A/B testing systems:

1. Stable assignment remains persisted and cached.
2. Partial rollout is now correct: rollout eligibility and variant selection use separate deterministic hash purposes. Variant weights are independent from rollout percentage and sum to 10000 basis points.
3. Users outside rollout receive a deterministic non-assignment response with `reason = OUT_OF_TRAFFIC_ALLOCATION`.
4. Existing persisted assignments are honored before rollout eligibility is rechecked to avoid user churn.
5. New experiments use a random persisted assignment salt rather than a predictable key-derived salt.
6. Event metadata is sanitized before PostgreSQL persistence and Kafka publication.
7. Kafka outbox publishing now claims rows atomically with `FOR UPDATE SKIP LOCKED`, which is safer for multi-instance deployments.
8. Metrics now include a dashboard-oriented summary endpoint with exposure count, event counts, total values, and conversion rates.
9. Personalized flag responses receive no-store cache headers.
10. Application configuration validates minimum admin-token and HMAC-secret lengths.

## Why partial rollout changed

The first version used the traffic allocation value as the modulo for selecting a variant. That gives stable assignments, but it is semantically wrong for partial rollouts because a 10% rollout can bias users into the earliest variant buckets.

The corrected model is:

1. Hash `experimentKey + salt + traffic + userHash` into `[0, 9999]`.
2. If the bucket is greater than or equal to `trafficAllocationBp`, return no assignment.
3. Hash `experimentKey + salt + variant + userHash` into `[0, totalVariantWeight - 1]`.
4. Select the variant using cumulative weights.

## Important production trade-offs

- Raw `userId` is still accepted at the trusted service boundary, but only HMAC-SHA256 hashes are persisted or published.
- Metadata redaction is a guardrail, not a substitute for API contracts. Real production clients should be contractually prevented from sending PII in event metadata.
- The static admin token is intentionally simple for this assignment. Replace it with OAuth2/OIDC, mTLS, or gateway policy for real production.
- The outbox gives at-least-once Kafka publishing. Downstream consumers should remain idempotent.
