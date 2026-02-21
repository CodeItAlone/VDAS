package vdas.safety;

import vdas.intent.Intent;

import java.util.Scanner;
import java.util.Set;

/**
 * Manages user confirmation for dangerous or medium-confidence commands.
 *
 * Prints a confirmation prompt and reads a single yes/no response.
 * No retry, no context memory, no conversational language.
 */
public class ConfirmationManager {

    private static final Set<String> ACCEPTED = Set.of("yes", "yeah", "confirm");

    /**
     * Requests confirmation from the user for the given intent.
     *
     * Prints: "Are you sure you want to &lt;action&gt;? (yes / no)"
     * where &lt;action&gt; is the resolved command name.
     *
     * @param intent  the intent requiring confirmation
     * @param scanner the input source (keyboard or redirected stdin)
     * @return true if the user confirmed, false otherwise
     */
    public boolean confirm(Intent intent, Scanner scanner) {
        String action = intent.getResolvedCommand()
                .map(cmd -> cmd.getName())
                .orElse("unknown");

        System.out.println("Are you sure you want to " + action + "? (yes / no)");

        String response = scanner.nextLine().trim().toLowerCase();
        return ACCEPTED.contains(response);
    }
}
