package br.com.economiamod.server.security;

public record PasswordHash(String algorithm, String saltBase64, String hashBase64) {
}

