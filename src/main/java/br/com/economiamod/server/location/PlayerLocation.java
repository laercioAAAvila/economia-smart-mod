package br.com.economiamod.server.location;

import java.util.UUID;

public record PlayerLocation(UUID id, UUID playerUuid, String name, String dimension, int x, int y, int z) {
}
