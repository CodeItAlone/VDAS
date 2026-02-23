package vdas.skill;

import vdas.intent.Intent;

import java.io.IOException;
import java.util.Map;

/**
 * Handles contextual browser navigation.
 * 
 * Activated when the user issues a command that translates into navigating
 * to a website inside an already-running browser.
 */
public class BrowserSkill implements Skill {

    // Hardcoded whitelist of supported browsers.
    // In a real system, this would be loaded from config.
    private static final Map<String, String> ALLOWED_BROWSERS = Map.of(
            "chrome", "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
            "edge", "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe");

    // Hardcoded whitelist of supported websites.
    private static final Map<String, String> ALLOWED_WEBSITES = Map.of(
            "youtube", "https://www.youtube.com",
            "google", "https://www.google.com",
            "github", "https://github.com");

    @Override
    public boolean canHandle(Intent intent) {
        if (intent.getResolvedCommand().isEmpty()
                || !intent.getResolvedCommand().get().getName().equals("open-app")) {
            return false;
        }

        Map<String, String> params = intent.getParameters();

        // 1. Must contain "app" (browser name) and it must be whitelisted
        String appName = params.get("app");
        if (appName == null || !ALLOWED_BROWSERS.containsKey(appName.toLowerCase())) {
            return false;
        }

        // 2. Must contain "url" (website key) and it must be whitelisted
        String urlKey = params.get("url");
        if (urlKey == null || !ALLOWED_WEBSITES.containsKey(urlKey.toLowerCase())) {
            return false;
        }

        return true;
    }

    @Override
    public void execute(Intent intent) {
        String appName = intent.getParameters().get("app").toLowerCase();
        String urlKey = intent.getParameters().get("url").toLowerCase();

        String browserPath = ALLOWED_BROWSERS.get(appName);
        String websiteUrl = ALLOWED_WEBSITES.get(urlKey);

        if (browserPath == null || websiteUrl == null) {
            System.err.println("[BROWSER] Rejected: invalid app or url context.");
            return;
        }

        try {
            System.out.println("[BROWSER] Navigating: " + appName + " → " + websiteUrl);
            new ProcessBuilder(browserPath, websiteUrl).start();
            System.out.println("[BROWSER] Successfully navigated to: " + websiteUrl);
        } catch (IOException e) {
            System.err.println("[BROWSER] Failed to navigate to " + websiteUrl + ": " + e.getMessage());
        }
    }
}
