package br.com.economiamod.common.account;

import java.text.Normalizer;
import java.util.Locale;

public final class AccountNameNormalizer {
    private AccountNameNormalizer() {
    }

    public static String normalize(String username) {
        if (username == null) {
            throw new IllegalArgumentException("username cannot be null");
        }

        String trimmed = username.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("username cannot be empty");
        }

        return Normalizer.normalize(trimmed, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
    }
}

