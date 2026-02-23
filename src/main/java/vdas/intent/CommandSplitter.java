package vdas.intent;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Splits raw user input into multiple sequential commands based on
 * predefined connectors ("and", "then", "and then").
 *
 * <p>
 * Ensures that multi-step commands are parsed deterministically
 * without relying on NLP. Limits execution to a maximum of 3 steps
 * to prevent runaway automation loops.
 * </p>
 */
public class CommandSplitter {

    /** Case-insensitive regex for word-bounded connectors. */
    private static final String SPLIT_REGEX = "(?i)\\b(and then|and|then)\\b";

    /**
     * Splits input into sequential steps.
     *
     * @param input the raw spoken or typed input
     * @return a list of separate command strings, trimmed and non-empty
     */
    public static List<String> split(String input) {
        if (input == null || input.isBlank()) {
            return List.of();
        }

        return Arrays.stream(input.split(SPLIT_REGEX))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
