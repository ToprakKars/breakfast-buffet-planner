package dev.toprak.breakfast;

/**
 * One item on the buffet.
 *
 * @param name      e.g. "Scrambled eggs"
 * @param category  where it sits on the buffet
 * @param unit      unit of the amounts, e.g. "kg", "pcs", "l"
 * @param perAdult  average amount one adult guest takes
 * @param perChild  average amount one child takes
 * @param packSize  how much one ordered pack contains (in the same unit)
 */
public record MenuItem(String name, Category category, String unit,
                       double perAdult, double perChild, double packSize) {

    public MenuItem {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is required");
        if (perAdult < 0 || perChild < 0) throw new IllegalArgumentException("amounts must not be negative");
        if (packSize <= 0) throw new IllegalArgumentException("packSize must be positive: " + name);
    }

    /** Items counted in pieces are always planned in whole numbers. */
    public boolean isCounted() {
        return "pcs".equals(unit);
    }

    /** Hot food is cooked in batches during service, cold food is mostly set up once. */
    public boolean isRefilledInBatches() {
        return category == Category.HOT || category == Category.BAKERY;
    }
}
