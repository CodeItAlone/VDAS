package vdas.executor;

import vdas.model.SystemCommand;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Executes a SystemCommand using ProcessBuilder on Windows.
 */
public class CommandExecutor {

    /**
     * Executes the given system command, printing stdout and stderr to the console.
     *
     * @param systemCommand the command to execute
     * @return the process exit code
     */
    public int execute(SystemCommand systemCommand) {
        System.out.println("─── Executing: " + systemCommand.getName() + " ───");
        System.out.println("Command : " + systemCommand.getCommand());

        try {
            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", systemCommand.getCommand());
            pb.redirectErrorStream(true); // merge stderr into stdout

            // Set working directory if specified
            if (systemCommand.getWorkingDirectory() != null && !systemCommand.getWorkingDirectory().isBlank()) {
                File workDir = new File(systemCommand.getWorkingDirectory());
                if (!workDir.exists()) {
                    System.err.println("[ERROR] Working directory does not exist: " + workDir.getAbsolutePath());
                    return -1;
                }
                if (!workDir.isDirectory()) {
                    System.err.println("[ERROR] Path is not a directory: " + workDir.getAbsolutePath());
                    return -1;
                }
                pb.directory(workDir);
                System.out.println("WorkDir : " + workDir.getAbsolutePath());
            }

            System.out.println("─── Output ───");

            Process process = pb.start();

            // Read combined output line by line
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }

            int exitCode = process.waitFor();
            System.out.println("─── Exit Code: " + exitCode + " ───");
            return exitCode;

        } catch (IOException e) {
            System.err.println("[ERROR] Failed to execute command: " + e.getMessage());
            return -1;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("[ERROR] Command interrupted: " + e.getMessage());
            return -1;
        }
    }
}
