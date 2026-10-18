package br.com.petrick.fornalhastech.client;

/** Client-only visual clock. One instance per visible block; never saved or synchronized. */
public final class EnergyFlowPhase {
    public static final int FRAMES = 5;
    public static final double IDLE_TICKS = 12.0;
    public static final double ACTIVE_TICKS = 4.0;
    private double phase;
    private double lastTime = Double.NaN;
    private boolean previousActive;

    public double sample(double time, boolean active) {
        if (!Double.isNaN(lastTime)) {
            double elapsed = Math.max(0.0, time - lastTime);
            phase = (phase + elapsed / (previousActive ? ACTIVE_TICKS : IDLE_TICKS)) % FRAMES;
        }
        lastTime = time;
        previousActive = active;
        return phase;
    }
}
