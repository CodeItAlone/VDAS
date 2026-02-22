package vdas;

import vdas.config.CommandLoader;
import vdas.executor.CommandExecutor;
import vdas.intent.DefaultAmbiguityDetector;
import vdas.intent.Intent;
import vdas.intent.IntentResolver;
import vdas.model.SystemCommand;
import vdas.safety.ClarificationPrompt;
import vdas.safety.ConfirmationManager;
import vdas.safety.DefaultDangerClassifier;
import vdas.safety.ExecutionGate;
import vdas.skill.AppLauncherSkill;
import vdas.skill.FileSystemSkill;
import vdas.skill.Skill;
import vdas.skill.SkillRegistry;
import vdas.skill.SystemInfoSkill;
import vdas.session.SessionContext;
import vdas.speech.SpeechInput;
import vdas.speech.WhisperSpeechInput;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * Main entry point for the VDAS application.
 * Version: 3.0 — Whisper STT + Keyboard input
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("   VDAS - Voice-Driven Automation System ");
        System.out.println("========================================");

        CommandLoader loader = new CommandLoader();
        CommandExecutor executor = new CommandExecutor();
        Scanner scanner = new Scanner(System.in);
        SessionContext sessionContext = new SessionContext();

        // ── Skill registration (explicit, no reflection) ──
        SkillRegistry skillRegistry = new SkillRegistry(List.of(
                new AppLauncherSkill(),
                new SystemInfoSkill(executor),
                new FileSystemSkill(executor)));

        SpeechInput speechInput = null;
        boolean voiceMode = false;

        try {
            List<SystemCommand> commands = loader.loadCommands();
            IntentResolver intentResolver = new IntentResolver(commands);

            // ---------- CLI ARG MODE ----------
            if (args.length > 0) {
                String rawInput = args[0];
                Intent intent = intentResolver.resolve(rawInput);

                ExecutionGate gate = new ExecutionGate(new DefaultDangerClassifier(), new DefaultAmbiguityDetector());
                ExecutionGate.Decision decision = gate.evaluate(intent);

                if (decision == ExecutionGate.Decision.REJECT) {
                    System.out.println("[REJECTED] Low confidence: \"" + rawInput + "\"");
                    return;
                }

                // CLI mode: no confirmation or clarification possible
                if (decision == ExecutionGate.Decision.CONFIRM || decision == ExecutionGate.Decision.CLARIFY) {
                    System.out.println("[REJECTED] Command requires interaction (not available in CLI mode): \""
                            + rawInput + "\"");
                    return;
                }

                Optional<Skill> skill = skillRegistry.findSkill(intent);
                if (skill.isPresent()) {
                    skill.get().execute(intent);
                    sessionContext.update(intent,
                            intent.getResolvedCommand().orElseThrow(), skill.get());
                } else {
                    System.err.println("[WARN] No skill found for: " + intent);
                }
                return;
            }

            // ---------- INPUT MODE SELECTION ----------
            System.out.println("\nSelect input mode:");
            System.out.println("  [W] Whisper voice (offline, microphone)");
            System.out.println("  [K] Keyboard (default)");
            System.out.print("> ");

            String mode = scanner.nextLine().trim();

            if (mode.equalsIgnoreCase("w")) {
                WhisperSpeechInput whisper = new WhisperSpeechInput();
                System.out.println("[INFO] Checking Whisper STT service...");

                if (whisper.isServiceAvailable()) {
                    speechInput = whisper;
                    voiceMode = true;
                    System.out.println("[INFO] Whisper STT service connected. Voice input mode activated.");
                } else {
                    System.err.println("[WARN] Whisper STT service is not running at http://localhost:8000");
                    System.out.println("[INFO] Start the Python server: cd whisper-stt && python server.py");
                    System.out.println("[INFO] Falling back to keyboard input.");
                }
            } else {
                System.out.println("[INFO] Keyboard input mode selected.");
            }

            // ---------- MAIN LOOP ----------
            listCommands(commands);
            interactiveSession(
                    intentResolver,
                    skillRegistry,
                    commands,
                    scanner,
                    voiceMode ? speechInput : null,
                    sessionContext);

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
            IntentResolver intentResolver,
            SkillRegistry skillRegistry,
            List<SystemCommand> commands,
            Scanner scanner,
            SpeechInput speechInput,
            SessionContext sessionContext) {

        boolean voiceMode = (speechInput != null);
        ExecutionGate gate = new ExecutionGate(new DefaultDangerClassifier(), new DefaultAmbiguityDetector());
        ConfirmationManager confirmationMgr = new ConfirmationManager();
        ClarificationPrompt clarificationPrompt = new ClarificationPrompt();

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

            // ── Keyboard shortcut: q / quit ──
            // These bypass the safety gate by design.
            // Voice / intent-based "quit" must always pass through ExecutionGate.
            if (input.equalsIgnoreCase("q") || input.equalsIgnoreCase("quit")) {
                System.out.println("Exiting VDAS...");
                break;
            }

            if (input.isEmpty()) {
                continue;
            }

            // ---------- COMMAND RESOLUTION → EXECUTION GATE → SKILL DISPATCH ----------
            Intent intent;
            try {
                int index = Integer.parseInt(input) - 1;
                if (index >= 0 && index < commands.size()) {
                    intent = intentResolver.resolveByCommand(input, commands.get(index));
                } else {
                    System.out.println("[WARN] Invalid index: " + input);
                    listCommands(commands);
                    continue;
                }
            } catch (NumberFormatException e) {
                intent = intentResolver.resolve(input);
            }

            // ── Execution gate ──
            ExecutionGate.Decision decision = gate.evaluate(intent);

            if (decision == ExecutionGate.Decision.REJECT) {
                System.out.println("[REJECTED] Low confidence: \"" + input + "\"");
                listCommands(commands);
                continue;
            }

            if (decision == ExecutionGate.Decision.CLARIFY) {
                Optional<SystemCommand> clarified = clarificationPrompt.ask(intent, scanner);
                if (clarified.isPresent()) {
                    // Refinement 2: Re-gate the clarified intent
                    Intent clarifiedIntent = intent.withResolvedCommand(clarified.get());
                    ExecutionGate.Decision clarifiedDecision = gate.evaluate(clarifiedIntent);

                    if (clarifiedDecision == ExecutionGate.Decision.REJECT) {
                        System.out.println("[REJECTED] Clarified command rejected.");
                        continue;
                    }

                    if (clarifiedDecision == ExecutionGate.Decision.CONFIRM) {
                        if (!confirmationMgr.confirm(clarifiedIntent, scanner)) {
                            System.out.println("[CANCELLED] Command cancelled by user.");
                            continue;
                        }
                    }

                    // Dispatch clarified intent
                    Optional<Skill> clarifiedSkill = skillRegistry.findSkill(clarifiedIntent);
                    if (clarifiedSkill.isPresent()) {
                        clarifiedSkill.get().execute(clarifiedIntent);
                        sessionContext.update(clarifiedIntent,
                                clarified.get(), clarifiedSkill.get());
                    } else {
                        System.out.println("[WARN] No skill found for: " + clarifiedIntent);
                    }
                } else {
                    System.out.println("[REJECTED] Ambiguous intent.");
                }
                continue;
            }

            if (decision == ExecutionGate.Decision.CONFIRM) {
                if (!confirmationMgr.confirm(intent, scanner)) {
                    System.out.println("[CANCELLED] Command cancelled by user.");
                    continue;
                }
            }

            // ── Skill dispatch (EXECUTE or confirmed CONFIRM) ──
            Optional<Skill> skill = skillRegistry.findSkill(intent);
            if (skill.isPresent()) {
                skill.get().execute(intent);
                sessionContext.update(intent,
                        intent.getResolvedCommand().orElseThrow(), skill.get());
            } else {
                System.out.println("[WARN] No skill found for: " + intent);
                listCommands(commands);
            }
        }
    }
}