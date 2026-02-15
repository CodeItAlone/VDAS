package vdas.config;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import vdas.intent.IntentNormalizer;
import vdas.model.SystemCommand;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Loads system commands from the classpath resource commands.json.
 */
public class CommandLoader {

    private static final String CONFIG_FILE = "commands.json";

    /**
     * Reads and parses commands.json from the classpath.
     *
     * @return list of SystemCommand objects
     * @throws IOException if the config file is missing or unreadable
     */
    public List<SystemCommand> loadCommands() throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (is == null) {
                throw new IOException("Configuration file '" + CONFIG_FILE + "' not found on classpath.");
            }
            try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                Type listType = new TypeToken<List<SystemCommand>>() {
                }.getType();
                List<SystemCommand> commands = new Gson().fromJson(reader, listType);
                if (commands == null || commands.isEmpty()) {
                    throw new IOException("No commands found in '" + CONFIG_FILE + "'.");
                }
                return commands;
            }
        }
    }

    /**
     * Finds a command by its name using normalized comparison.
     * "list files" (voice) matches "list-files" (config).
     *
     * @param commands list of available commands
     * @param name     raw user input (voice or keyboard)
     * @return the matching SystemCommand, or null if not found
     */
    public SystemCommand findByName(List<SystemCommand> commands, String name) {
        String normalizedInput = IntentNormalizer.normalize(name);
        return commands.stream()
                .filter(cmd -> IntentNormalizer.normalizeCommandName(cmd.getName())
                        .equals(normalizedInput))
                .findFirst()
                .orElse(null);
    }
}
