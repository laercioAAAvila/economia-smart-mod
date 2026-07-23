package br.com.economiamod.server.commercial.inventory;

import br.com.economiamod.common.pricing.ComparisonMode;
import java.util.Objects;

public final class CommercialItemMatcher {
    public boolean matches(CommercialItemSnapshot actual, CommercialItemSnapshot expected, ComparisonMode mode) {
        if (actual == null || expected == null || actual.isEmpty() || expected.isEmpty()) {
            return false;
        }
        if (!Objects.equals(actual.itemId(), expected.itemId())) {
            return false;
        }
        return mode == ComparisonMode.ITEM_ID_ONLY || sameComponents(actual, expected);
    }

    public boolean stackable(CommercialItemSnapshot actual, CommercialItemSnapshot candidate) {
        return matches(actual, candidate, ComparisonMode.FULL_COMPONENTS);
    }

    private boolean sameComponents(CommercialItemSnapshot actual, CommercialItemSnapshot expected) {
        return Objects.equals(actual.components(), expected.components())
                && Objects.equals(actual.dataVersion(), expected.dataVersion());
    }
}
