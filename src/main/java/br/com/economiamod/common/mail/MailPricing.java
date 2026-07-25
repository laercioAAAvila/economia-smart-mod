package br.com.economiamod.common.mail;

public final class MailPricing {
    private MailPricing() {
    }

    public static long horizontalDistance(int originX, int originZ, int destinationX, int destinationZ) {
        long dx = (long) originX - destinationX;
        long dz = (long) originZ - destinationZ;
        return (long) Math.floor(Math.sqrt((double) dx * dx + (double) dz * dz));
    }

    public static int distancePercent(long distance) {
        if (distance < 1000L) {
            return 0;
        }
        if (distance <= 5000L) {
            return Math.min(50, (int) ((distance - 1000L) / 80L));
        }
        if (distance <= 10000L) {
            return Math.min(150, 51 + (int) ((distance - 5001L) / 50L));
        }
        if (distance <= 30000L) {
            return clamp(151 + (int) (((distance - 10001L) * 149L) / 19999L), 151, 300);
        }
        if (distance <= 70000L) {
            return clamp(301 + (int) (((distance - 30001L) * 99L) / 39999L), 301, 400);
        }
        return 500;
    }

    public static long total(long pricePerSlot, int occupiedSlots, long distance) {
        long base = Math.multiplyExact(Math.max(0L, pricePerSlot), Math.max(0, occupiedSlots));
        int percent = distancePercent(distance);
        return Math.addExact(base, (base * percent) / 100L);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
