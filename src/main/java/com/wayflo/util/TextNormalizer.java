package com.wayflo.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class TextNormalizer {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_SEARCH_CHARS = Pattern.compile("[^a-z0-9 ]");
    private static final Pattern SPACES = Pattern.compile("\\s+");

    public String normalize(String value) {
        if (value == null) {
            return "";
        }
        String ascii = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace('đ', 'd')
            .replace('Đ', 'D');
        return SPACES.matcher(
                NON_SEARCH_CHARS.matcher(
                    DIACRITICS.matcher(ascii).replaceAll("")
                        .toLowerCase(Locale.ROOT)
                ).replaceAll(" ")
            )
            .replaceAll(" ")
            .trim();
    }
}
