package br.com.petrick.fornalhastech.blockentity;

/** Integer native burn/recipe ticks: changing cores never rescales stored fuel. */
public final class FuelWork {
    private FuelWork() {}
    public static int step(int speed, int progress, int total) {
        return (int) Math.min(Math.max(1, speed), Math.max(0L, (long) total - progress));
    }
    /** Prime the next fuel at a tick boundary only when valid recipe work remains. */
    public static boolean shouldPrimeNextTick(int remainingBurn, boolean recipeWorkRemains) {
        return remainingBurn <= 0 && recipeWorkRemains;
    }
}
