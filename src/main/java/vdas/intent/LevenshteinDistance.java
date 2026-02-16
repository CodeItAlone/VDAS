package vdas.intent;

/**
 * Utility class for computing Levenshtein (edit) distance and
 * similarity score between two strings.
 *
 * Pure Java — no third-party libraries.
 */
public class LevenshteinDistance {

    private LevenshteinDistance() {
        // utility class
    }

    /**
     * Computes the minimum number of single-character edits
     * (insertions, deletions, substitutions) required to transform
     * string {@code a} into string {@code b}.
     *
     * Uses the standard dynamic-programming approach with O(min(m,n)) space.
     *
     * @param a first string (non-null)
     * @param b second string (non-null)
     * @return the edit distance (≥ 0)
     */
    public static int compute(String a, String b) {
        if (a == null || b == null) {
            throw new IllegalArgumentException("Inputs must not be null");
        }

        // Ensure 'a' is the shorter string for space optimization
        if (a.length() > b.length()) {
            String tmp = a;
            a = b;
            b = tmp;
        }

        int m = a.length();
        int n = b.length();

        // previous and current row of distances
        int[] prev = new int[m + 1];
        int[] curr = new int[m + 1];

        // Base case: transforming empty string to a[0..i]
        for (int i = 0; i <= m; i++) {
            prev[i] = i;
        }

        for (int j = 1; j <= n; j++) {
            curr[0] = j;
            for (int i = 1; i <= m; i++) {
                int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
                curr[i] = Math.min(
                        Math.min(curr[i - 1] + 1, prev[i] + 1), // insert, delete
                        prev[i - 1] + cost // substitute
                );
            }
            // swap rows
            int[] temp = prev;
            prev = curr;
            curr = temp;
        }

        return prev[m];
    }

    /**
     * Computes the similarity between two strings as a value in [0.0, 1.0].
     *
     * Similarity = 1.0 − (editDistance / maxLength).
     * Two identical strings yield 1.0; completely different strings approach 0.0.
     *
     * @param a first string (non-null)
     * @param b second string (non-null)
     * @return similarity score between 0.0 and 1.0
     */
    public static double similarity(String a, String b) {
        if (a == null || b == null) {
            throw new IllegalArgumentException("Inputs must not be null");
        }
        int maxLen = Math.max(a.length(), b.length());
        if (maxLen == 0) {
            return 1.0; // both empty
        }
        int distance = compute(a, b);
        return 1.0 - ((double) distance / maxLen);
    }
}
