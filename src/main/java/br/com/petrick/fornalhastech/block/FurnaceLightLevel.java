package br.com.petrick.fornalhastech.block;

/** Approved block-light levels based on selected visual appearance and active fuel burn. */
public final class FurnaceLightLevel {
    private FurnaceLightLevel() {}
    public static int get(int visualColor, boolean burning) {
        boolean coloredAppearance = visualColor > 0;
        return coloredAppearance ? (burning ? 15 : 10) : (burning ? 10 : 0);
    }
}

