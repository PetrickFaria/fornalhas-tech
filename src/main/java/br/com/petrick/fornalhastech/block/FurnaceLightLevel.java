package br.com.petrick.fornalhastech.block;

/** Approved block-light levels based on installed core and active fuel burn. */
public final class FurnaceLightLevel {
    private FurnaceLightLevel() {}
    public static int get(boolean hasCore, boolean burning) {
        return hasCore ? (burning ? 15 : 10) : (burning ? 5 : 0);
    }
}
