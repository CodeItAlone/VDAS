package vdas;

import vdas.config.CommandLoader;
import vdas.executor.CommandExecutor;
import vdas.model.SystemCommand;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

/**
 * Main entry point for the VDAS application.
 * Version: 1.0.1
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("   VDAS - Voice-Driven Automation System  ");
        System.out.println("========================================");

        CommandLoader loader = new CommandLoader();
        CommandExecutor executor = new CommandExecutor();

        try {
            List<SystemCommand> commands = loader.loadCommands();
            
            if (args.length > 0) {
                // Execute command passed as argument
                String commandName = args[0];
                SystemCommand cmd = loader.findByName(commands, commandName);
                if (cmd != null) {
                    executor.execute(cmd);
                } else {
                    System.err.println("[ERROR] Command not found: " + commandName);
                }
            } else {
                // Interactive mode or list available commands
                listCommands(commands);
                interactiveSession(loader, executor, commands);
            }

        } catch (IOException e) {
            System.err.println("[ERROR] Failed to start VDAS: " + e.getMessage());
        }
    }

    private static void listCommands(List<SystemCommand> commands) {
        System.out.println("\nAvailable Commands:");
        for (int i = 0; i < commands.size(); i++) {
            System.out.println((i + 1) + ". " + commands.get(i).getName());
        }
        System.out.println("Q. Quit");
    }

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("\nSelect a command (name or number) > ");
            if (!scanner.hasNextLine()) break;
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("q") || input.equalsIgnoreCase("quit")) {
                System.out.println("Exiting VDAS...");
                break;
            }

            if (input.isEmpty()) continue;

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
                System.out.println("Invalid selection. Please try again.");
                listCommands(commands);
            }
        }
    }
}
