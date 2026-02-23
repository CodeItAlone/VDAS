package vdas.skill;

import vdas.intent.Intent;

import java.io.IOException;
import java.util.Map;

/**
 * Skill for launching whitelisted desktop applications.
 *
 * <p>
 * This skill handles intents resolved to the "open-app" command.
 * It reads the app name exclusively from
 * {@code intent.getParameters().get("app")}
 * — no string parsing is performed inside this skill.
 *
 * <p>
 * The "open-app" command has an intentionally empty {@code command} field in
 * commands.json. It is executed exclusively by this skill via
 * {@link ProcessBuilder}
 * and must never be passed to {@link vdas.executor.CommandExecutor}.
 *
 * <p>
 * Stateless — the whitelist is a final, unmodifiable map.
 */
public class AppLauncherSkill implements Skill {

    private static final String OPEN_APP_COMMAND = "open-app";

    /**
     * Whitelisted applications: canonical name → absolute executable path.
     * Only apps in this map may be launched. Keys are canonical app names,
     * not raw speech. Paths must be explicit and deterministic.
     */
    private static final Map<String, String> ALLOWED_APPS = Map.of(
            "chrome", "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
            "vscode", "C:\\Users\\SUBRATO KUNDU\\AppData\\Local\\Programs\\Microsoft VS Code\\Code.exe",
            "explorer", "explorer.exe",
            "notepad", "notepad.exe",
            "calculator", "calc.exe");

    @Override
    public boolean canHandle(Intent intent) {
        return intent.getResolvedCommand()
                .map(cmd -> OPEN_APP_COMMAND.equals(cmd.getName()))
                .orElse(false)
                && getWhitelistedPath(intent) != null;
    }

    @Override
    public void execute(Intent intent) {
        String appName = intent.getParameters().get("app");
        String path = ALLOWED_APPS.get(appName);

        if (path == null) {
            // Defensive — canHandle should prevent this
            System.err.println("[APP-LAUNCHER] Rejected: app not whitelisted: \"" + appName + "\"");
            return;
        }

        String url = intent.getParameters().get("url");

        try {
            if (url != null && !url.isEmpty()) {
                // Contextual navigation: launch browser with URL argument
                System.out.println("[APP-LAUNCHER] Navigating: " + appName + " → " + url);
                new ProcessBuilder(path, url).start();
                System.out.println("[APP-LAUNCHER] Successfully navigated " + appName + " to: " + url);
            } else {
                // Standard app launch
                System.out.println("[APP-LAUNCHER] Launching: " + appName + " → " + path);
                new ProcessBuilder(path).start();
                System.out.println("[APP-LAUNCHER] Successfully launched: " + appName);
            }
        } catch (IOException e) {
            System.err.println("[APP-LAUNCHER] Failed to launch " + appName + ": " + e.getMessage());
        }
    }

    /**
     * Returns the whitelisted path for the app in this intent, or null
     * if the app parameter is missing or not whitelisted.
     */
    private String getWhitelistedPath(Intent intent) {
        String appName = intent.getParameters().get("app");
        if (appName == null || appName.isEmpty()) {
            return null;
        }
        return ALLOWED_APPS.get(appName);
    }
}
