package br.com.petrick.fornalhastech.client;

/** Client-only visual clock. One instance per visible block; never saved or synchronized. */
public final class EnergyFlowPhase {
    public static final int FRAMES = 16;
    /** Original seven files remain unchanged; nine new images interpolate the largest cycle gaps. */
    public static final int[] FRAME_ORDER = {0, 7, 8, 1, 9, 5, 10, 2, 11, 3, 12, 13, 4, 14, 6, 15};
    public static final double IDLE_CYCLE_TICKS = 60.0;
    public static final double ACTIVE_CYCLE_TICKS = 20.0;
    private double phase;
    private double lastTime = Double.NaN;
    private boolean previousActive;

    public double sample(double time, boolean active) {
        if (!Double.isNaN(lastTime)) {
            double elapsed = Math.max(0.0, time - lastTime);
            double cycleTicks = previousActive ? ACTIVE_CYCLE_TICKS : IDLE_CYCLE_TICKS;
            phase = (phase + elapsed * FRAMES / cycleTicks) % FRAMES;
        }
        lastTime = time;
        previousActive = active;
        return phase;
    }
}

