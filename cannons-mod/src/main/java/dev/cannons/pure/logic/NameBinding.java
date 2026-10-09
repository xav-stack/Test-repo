package dev.cannons.pure.logic;

/** Exact, case-sensitive comparison for named cannon binding items. */
public final class NameBinding {
    private NameBinding() {
    }

    public static boolean matches(String customName, String username) {
        return customName != null && username != null && customName.equals(username);
    }
}
