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
    private static final int ITERATIONS = 210_000;
    private static final int KEY_BITS = 256;

    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordHash hash(char[] password) {
        byte[] salt = new byte[SALT_BYTES];
        secureRandom.nextBytes(salt);
        byte[] hash = derive(password, salt);
        return new PasswordHash(
                ALGORITHM,
                Base64.getEncoder().encodeToString(salt),
                Base64.getEncoder().encodeToString(hash)
        );
    }

    public boolean verify(char[] password, PasswordHash storedHash) {
        if (!ALGORITHM.equals(storedHash.algorithm())) {
            return false;
        }

        byte[] salt = Base64.getDecoder().decode(storedHash.saltBase64());
        byte[] expectedHash = Base64.getDecoder().decode(storedHash.hashBase64());
        byte[] actualHash = derive(password, salt);
        return MessageDigest.isEqual(expectedHash, actualHash);
    }

    private byte[] derive(char[] password, byte[] salt) {
        try {
            PBEKeySpec keySpec = new PBEKeySpec(password, salt, ITERATIONS, KEY_BITS);
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(keySpec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException exception) {
            throw new IllegalStateException("Unable to hash banking password", exception);
        }
    }
}

