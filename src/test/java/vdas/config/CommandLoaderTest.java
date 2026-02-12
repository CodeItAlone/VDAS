package vdas.config;

import org.junit.jupiter.api.Test;
import vdas.model.SystemCommand;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CommandLoaderTest {

    @Test
    void testLoadCommands() throws IOException {
        CommandLoader loader = new CommandLoader();
        List<SystemCommand> commands = loader.loadCommands();

        assertNotNull(commands);
        assertFalse(commands.isEmpty());

        // Verify at least one expected command exists
        boolean found = commands.stream().anyMatch(c -> c.getName().equals("list-files"));
        assertTrue(found, "Expected command 'list-files' not found in commands.json");
    }

    @Test
    void testFindByName() {
        CommandLoader loader = new CommandLoader();
        List<SystemCommand> mockCommands = List.of(
                new SystemCommand("test", "echo test", null));

        SystemCommand found = loader.findByName(mockCommands, "TEST");
        assertNotNull(found);
        assertEquals("test", found.getName());

        assertNull(loader.findByName(mockCommands, "non-existent"));
    }
}
