package dev.toprak.breakfast;

public enum Category {
    HOT, BAKERY, COLD, FRUIT, DRINKS;

    public static Category parse(String text) {
        try {
            return valueOf(text.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown category: " + text);
        }
    }
}
