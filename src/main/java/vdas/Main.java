package vdas;

import vdas.config.CommandLoader;
import vdas.executor.CommandExecutor;
import vdas.model.SystemCommand;
import vdas.speech.SpeechInput;
import vdas.speech.VoskSpeechInput;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

/**
 * Main entry point for the VDAS application.
 * Version: 2.0 — Voice + Keyboard input
 */
public class Main {

    private static final String VOSK_MODEL_PATH =
            "C:\\vosk-models\\vosk-model-small-en-us-0.15";

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("   VDAS - Voice-Driven Automation System ");
        System.out.println("========================================");

        CommandLoader loader = new CommandLoader();
        CommandExecutor executor = new CommandExecutor();
        Scanner scanner = new Scanner(System.in);

        SpeechInput speechInput = null;
        boolean voiceMode = false;

        try {
            List<SystemCommand> commands = loader.loadCommands();

            // ---------- CLI ARG MODE ----------
            if (args.length > 0) {
                String commandName = args[0];
                SystemCommand cmd = loader.findByName(commands, commandName);

                if (cmd != null) {
                    executor.execute(cmd);
                } else {
                    System.err.println("[ERROR] Command not found: " + commandName);
                }
                return;
            }

            // ---------- INPUT MODE SELECTION ----------
            System.out.println("\nSelect input mode:");
            System.out.println("  [V] Voice (offline, microphone)");
            System.out.println("  [K] Keyboard (default)");
            System.out.print("> ");

            String mode = scanner.nextLine().trim();

            if (mode.equalsIgnoreCase("v")) {
                try {
                    speechInput = new VoskSpeechInput(VOSK_MODEL_PATH);
                    voiceMode = true;
                    System.out.println("[INFO] Voice input mode activated.");
                } catch (Exception e) {
                    System.err.println("[WARN] Voice input unavailable: " + e.getMessage());
                    System.out.println("[INFO] Falling back to keyboard input.");
                }
            } else {
                System.out.println("[INFO] Keyboard input mode selected.");
            }

            // ---------- MAIN LOOP ----------
            listCommands(commands);
            interactiveSession(
                    loader,
                    executor,
                    commands,
                    scanner,
                    voiceMode ? speechInput : null
            );

        } catch (IOException e) {
            System.err.println("[ERROR] Failed to start VDAS: " + e.getMessage());
        } finally {
            // ---------- CLEANUP ----------
            if (speechInput != null) {
                speechInput.close();
            }
            // DO NOT close scanner(System.in)
        }
    }

    private static void listCommands(List<SystemCommand> commands) {
        System.out.println("\nAvailable Commands:");
        for (int i = 0; i < commands.size(); i++) {
            System.out.println((i + 1) + ". " + commands.get(i).getName());
        }
        System.out.println("Q. Quit");
    }

    private static void interactiveSession(
            CommandLoader loader,
            CommandExecutor executor,
            List<SystemCommand> commands,
            Scanner scanner,
            SpeechInput speechInput
    ) {

        boolean voiceMode = (speechInput != null);

        while (true) {
            String input;

            // ---------- VOICE MODE ----------
            if (voiceMode) {
                System.out.println("\n[VOICE] Say command (or say/type 'k' to switch, 'q' to quit)");
                input = speechInput.listen();

                if (input.isEmpty()) {
                    System.out.print("No speech detected. Type command or press Enter to retry > ");
                    input = scanner.nextLine().trim();
                    if (input.isEmpty()) {
                        continue;
                    }
                } else {
                    System.out.println("[VOICE] Recognized: \"" + input + "\"");
                }

                if (input.equalsIgnoreCase("k") || input.equalsIgnoreCase("keyboard")) {
                    System.out.println("[INFO] Switched to keyboard input.");
                    voiceMode = false;
                    continue;
                }

            }
            // ---------- KEYBOARD MODE ----------
            else {
                System.out.print("\nSelect a command (name or number) > ");
                input = scanner.nextLine().trim();
            }

            if (input.equalsIgnoreCase("q") || input.equalsIgnoreCase("quit")) {
                System.out.println("Exiting VDAS...");
                break;
            }

            if (input.isEmpty()) {
                continue;
            }

            // ---------- COMMAND RESOLUTION ----------
            SystemCommand target = null;
            try {
                int index = Integer.parseInt(input) - 1;
                if (index >= 0 && index < commands.size()) {
                    target = commands.get(index);
                }
            } catch (NumberFormatException e) {
                target = loader.findByName(commands, input);
            }

            if (target != null) {
                executor.execute(target);
            } else {
                System.out.println("Invalid selection: \"" + input + "\"");
                listCommands(commands);
            }
        }
    }
}