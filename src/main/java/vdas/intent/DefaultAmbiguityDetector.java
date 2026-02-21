package vdas.intent;

/**
 * Score-based ambiguity detector.
 *
 * An intent is ambiguous when ALL of the following are true:
 * <ul>
 * <li>ConfidenceBand is MEDIUM</li>
 * <li>At least two candidate commands exist</li>
 * <li>The difference between the top two candidate scores is ≤ 0.10</li>
 * </ul>
 *
 * Candidates are expected to be sorted by descending score (Refinement 3).
 */
public class DefaultAmbiguityDetector implements AmbiguityDetector {

    private static final double AMBIGUITY_SCORE_GAP = 0.10;

    @Override
    public boolean isAmbiguous(Intent intent) {
        if (intent.getConfidenceBand() != ConfidenceBand.MEDIUM) {
            return false;
        }

        if (intent.getCandidateCommands().size() < 2) {
            return false;
        }

        double topScore = intent.getCandidateScores().get(0);
        double secondScore = intent.getCandidateScores().get(1);
        return (topScore - secondScore) <= AMBIGUITY_SCORE_GAP;
    }
}
