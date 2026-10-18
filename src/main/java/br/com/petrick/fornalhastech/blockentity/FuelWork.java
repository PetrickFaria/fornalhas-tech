package br.com.petrick.fornalhastech.blockentity;

/** Integer native burn/recipe ticks: changing cores never rescales stored fuel. */
public final class FuelWork {
    private FuelWork() {}
    public static int step(int speed, int progress, int total) {
        return (int) Math.min(Math.max(1, speed), Math.max(0L, (long) total - progress));
    }
}
