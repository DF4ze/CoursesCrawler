package fr.ses10doigts.coursesCrawler.model.telegram;

import java.util.Arrays;

public enum Verbose {
    HIGH, LOW;

    public static Verbose fromString(String value) {
        return Arrays.stream(values())
                .filter(v -> v.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Valeur inconnue : " + value));
    }
}
