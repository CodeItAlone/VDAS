package vdas.safety;

import vdas.intent.Intent;
import vdas.model.SystemCommand;

import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * Presents ambiguous candidate commands and reads the user's clarification.
 *
 * Prints a numbered list of candidates (sorted by descending score),
 * reads one line only. Accepts a number index or exact command name.
 * Anything else returns empty. No retries, no memory.
 */
public class ClarificationPrompt {

    /**
     * Asks the user to clarify which command they meant.
     *
     * @param intent  the ambiguous intent with candidate commands
     * @param scanner the input source
     * @return the selected command, or empty if no valid selection
     */
    public Optional<SystemCommand> ask(Intent intent, Scanner scanner) {
        List<SystemCommand> candidates = intent.getCandidateCommands();

        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        System.out.println("Did you mean:");
        for (int i = 0; i < candidates.size(); i++) {
            System.out.println((i + 1) + ". " + candidates.get(i).getName());
        }

        String response = scanner.nextLine().trim();

        if (response.isEmpty()) {
            return Optional.empty();
        }

        // Try numeric selection
        try {
            int index = Integer.parseInt(response) - 1;
            if (index >= 0 && index < candidates.size()) {
                return Optional.of(candidates.get(index));
            }
            return Optional.empty();
        } catch (NumberFormatException e) {
            // Try exact name match
            for (SystemCommand cmd : candidates) {
                if (cmd.getName().equalsIgnoreCase(response)) {
                    return Optional.of(cmd);
                }
            }
            return Optional.empty();
        }
    }
}
