package vdas.model;

/**
 * Represents a single system command loaded from commands.json.
 */
public class SystemCommand {

    private String name;
    private String command;
    private String workingDirectory; // optional

    public SystemCommand() {
    }

    public SystemCommand(String name, String command, String workingDirectory) {
        this.name = name;
        this.command = command;
        this.workingDirectory = workingDirectory;
    }

    public String getName() {
        return name;
    }

    public String getCommand() {
        return command;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    @Override
    public String toString() {
        return "SystemCommand{name='" + name + "', command='" + command + "'" +
                (workingDirectory != null ? ", workingDirectory='" + workingDirectory + "'" : "") +
                "}";
    }
}
