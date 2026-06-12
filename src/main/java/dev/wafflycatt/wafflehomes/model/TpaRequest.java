package dev.wafflycatt.wafflehomes.model;

import java.util.UUID;

/**
 * An in-memory TPA or TPHERE request between two players.
 *
 * @param from       UUID of the player who sent the request
 * @param to         UUID of the player who received the request
 * @param isHere     true = TPHERE (target teleports to sender); false = TPA (sender teleports to target)
 * @param expiresAt  System.currentTimeMillis() value when this request expires
 */
public record TpaRequest(
        UUID from,
        UUID to,
        boolean isHere,
        long expiresAt
) {
    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }
}
