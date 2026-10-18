package br.com.petrick.fornalhastech.client;

/** Client-only visual clock. One instance per visible block; never saved or synchronized. */
public final class EnergyFlowPhase {
    public static final int FRAMES = 7;
    /** Original frames stay in their source files; frame 5 tweens 1->2, frame 6 tweens 4->0. */
    public static final int[] FRAME_ORDER = {0, 1, 5, 2, 3, 4, 6};
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

