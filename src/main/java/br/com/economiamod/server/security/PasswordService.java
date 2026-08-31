package br.com.economiamod.server.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordService {
    public static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int SALT_BYTES = 16;
    private static final int LEGACY_ITERATIONS = 210_000;
    private static final int DEFAULT_ITERATIONS = 600_000;
    private static final int MIN_ACCEPTED_ITERATIONS = 100_000;
    private static final int MAX_ACCEPTED_ITERATIONS = 2_000_000;
    private static final int KEY_BITS = 256;

    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordHash hash(char[] password) {
        byte[] salt = new byte[SALT_BYTES];
        secureRandom.nextBytes(salt);
        byte[] hash = derive(password, salt, DEFAULT_ITERATIONS);
        return new PasswordHash(
                ALGORITHM + ":" + DEFAULT_ITERATIONS,
                Base64.getEncoder().encodeToString(salt),
                Base64.getEncoder().encodeToString(hash)
        );
    }

    public boolean verify(char[] password, PasswordHash storedHash) {
        int iterations = iterations(storedHash.algorithm());
        if (iterations <= 0) {
            return false;
        }

        try {
            byte[] salt = Base64.getDecoder().decode(storedHash.saltBase64());
            byte[] expectedHash = Base64.getDecoder().decode(storedHash.hashBase64());
            byte[] actualHash = derive(password, salt, iterations);
            return MessageDigest.isEqual(expectedHash, actualHash);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private int iterations(String storedAlgorithm) {
        if (ALGORITHM.equals(storedAlgorithm)) {
            return LEGACY_ITERATIONS;
        }
        String prefix = ALGORITHM + ":";
        if (storedAlgorithm == null || !storedAlgorithm.startsWith(prefix)) {
            return -1;
        }
        try {
            int parsed = Integer.parseInt(storedAlgorithm.substring(prefix.length()));
            return parsed >= MIN_ACCEPTED_ITERATIONS && parsed <= MAX_ACCEPTED_ITERATIONS ? parsed : -1;
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    private byte[] derive(char[] password, byte[] salt, int iterations) {
        PBEKeySpec keySpec = new PBEKeySpec(password, salt, iterations, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(keySpec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException exception) {
            throw new IllegalStateException("Unable to hash banking password", exception);
        } finally {
            keySpec.clearPassword();
        }
    }
}
