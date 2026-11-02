package com.thegildedking.thenull;

import java.util.Locale;

/** The four individually assigned commander positions. */
public enum Commander {
    SOVEREIGN("sovereign"),
    EXECUTIONER("executioner"),
    HARBINGER("harbinger"),
    WATCHER("watcher");

    private final String key;

    Commander(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    /** Parses "sovereign", "The Sovereign", "the_sovereign" etc. Returns null if unknown. */
    public static Commander parse(String input) {
        if (input == null) {
            return null;
        }
        String s = input.toLowerCase(Locale.ROOT).trim().replace('_', ' ').replace('-', ' ');
        if (s.startsWith("the ")) {
            s = s.substring(4).trim();
        }
        for (Commander c : values()) {
            if (c.key.equals(s)) {
                return c;
            }
        }
        return null;
    }
}
