package vdas.model;

import java.util.Collections;
import java.util.List;

/**
 * Represents a single system command loaded from commands.json.
 */
public class SystemCommand {

    private String name;
    private String command;
    private String workingDirectory; // optional
    private List<String> aliases; // optional synonyms for voice/fuzzy matching

    public SystemCommand() {
    }

    public SystemCommand(String name, String command, String workingDirectory) {
        this.name = name;
        this.command = command;
        this.workingDirectory = workingDirectory;
    }

    public SystemCommand(String name, String command, String workingDirectory, List<String> aliases) {
        this.name = name;
        this.command = command;
        this.workingDirectory = workingDirectory;
        this.aliases = aliases;
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

    /**
     * Returns the list of aliases, or an empty list if none are defined.
     */
    public List<String> getAliases() {
        return aliases != null ? aliases : Collections.emptyList();
    }

    @Override
    public String toString() {
        return "SystemCommand{name='" + name + "', command='" + command + "'" +
                (workingDirectory != null ? ", workingDirectory='" + workingDirectory + "'" : "") +
                (!getAliases().isEmpty() ? ", aliases=" + aliases : "") +
                "}";
    }
}
