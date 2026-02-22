package vdas.intent;

/**
 * Discrete confidence bands derived from numeric confidence scores.
 *
 * This enum is the SINGLE source of truth for mapping numeric confidence
 * values to categorical bands. It is assigned once during intent construction
 * in {@link IntentResolver} and only read downstream — never recomputed.
 *
 * <ul>
 * <li>{@code HIGH} — confidence == 1.0 (exact or alias match)</li>
 * <li>{@code MEDIUM} — confidence >= 0.75 and < 1.0 (fuzzy match above
 * threshold)</li>
 * <li>{@code LOW} — confidence < 0.75 (below threshold)</li>
 * </ul>
 */
public enum ConfidenceBand {
    HIGH,
    MEDIUM,
    LOW;

    /**
     * Maps a numeric confidence score to a discrete band.
     *
     * @param confidence score in [0.0, 1.0]
     * @return the corresponding ConfidenceBand
     */
    public static ConfidenceBand of(double confidence) {
        if (confidence >= 1.0) {
            return HIGH;
        } else if (confidence >= 0.75) {
            return MEDIUM;
        } else {
            return LOW;
        }
    }

    /**
     * Returns {@code true} if this band is at least as confident as the
     * given minimum band.
     *
     * <p>
     * Ordinal order: HIGH (0) &gt; MEDIUM (1) &gt; LOW (2).
     * A lower ordinal means higher confidence.
     * </p>
     *
     * @param minimum the minimum confidence band to check against
     * @return true if this band meets or exceeds the minimum
     */
    public boolean isAtLeast(ConfidenceBand minimum) {
        return this.ordinal() <= minimum.ordinal();
    }
}
