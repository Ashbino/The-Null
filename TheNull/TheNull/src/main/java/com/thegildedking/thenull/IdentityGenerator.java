package com.thegildedking.thenull;

import java.security.SecureRandom;
import java.util.Locale;

/** Generates unique random identities made of A-Z and 0-9. */
public final class IdentityGenerator {
    private static final String DEFAULT_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private final TheNullPlugin plugin;
    private final SecureRandom random = new SecureRandom();

    public IdentityGenerator(TheNullPlugin plugin) {
        this.plugin = plugin;
    }

    private String charset() {
        String raw = plugin.getConfig().getString("identity.characters", DEFAULT_CHARS)
                .toUpperCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder();
        for (char c : raw.toCharArray()) {
            boolean valid = (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9');
            if (valid && sb.indexOf(String.valueOf(c)) < 0) {
                sb.append(c);
            }
        }
        return sb.length() >= 2 ? sb.toString() : DEFAULT_CHARS;
    }

    /** Generates an identity not currently used by any registered Null. */
    public String generate(NullManager manager) {
        int min = Math.max(1, plugin.getConfig().getInt("identity.min-length", 5));
        int max = Math.max(1, plugin.getConfig().getInt("identity.max-length", 8));
        if (min > max) {
            int t = min;
            min = max;
            max = t;
        }
        min = Math.min(min, 16);
        max = Math.min(max, 16);
        String chars = charset();
        for (int attempt = 0; attempt < 5000; attempt++) {
            int len = min + random.nextInt(max - min + 1);
            StringBuilder sb = new StringBuilder(len);
            for (int i = 0; i < len; i++) {
                sb.append(chars.charAt(random.nextInt(chars.length())));
            }
            String id = sb.toString();
            if (!manager.isIdentityTaken(id)) {
                return id;
            }
        }
        throw new IllegalStateException("Could not generate a unique Null identity; "
                + "increase identity.max-length in config.yml");
    }
}
