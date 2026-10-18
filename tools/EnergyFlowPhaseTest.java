import br.com.petrick.fornalhastech.client.EnergyFlowPhase;

public class EnergyFlowPhaseTest {
    static void close(double actual, double expected) {
        if (Math.abs(actual - expected) > 1e-8) throw new AssertionError(actual + " != " + expected);
    }

    public static void main(String[] args) {
        if (EnergyFlowPhase.FRAMES != 7 || EnergyFlowPhase.FRAME_ORDER.length != 7) {
            throw new AssertionError("Expected seven animation steps");
        }
        int[] expectedOrder = {0, 1, 5, 2, 3, 4, 6};
        for (int i = 0; i < expectedOrder.length; i++) {
            if (EnergyFlowPhase.FRAME_ORDER[i] != expectedOrder[i]) throw new AssertionError("Frame order changed");
        }

        EnergyFlowPhase idle = new EnergyFlowPhase();
        idle.sample(0, false);
        close(idle.sample(60, false), 0);
        close(idle.sample(60 + 60.0 / 7.0, false), 1);

        EnergyFlowPhase active = new EnergyFlowPhase();
        active.sample(0, true);
        close(active.sample(20.0 / 7.0, true), 1);
        close(active.sample(40.0 / 7.0, true), 2);
        close(active.sample(20, true), 0); // full active cycle duration is preserved

        EnergyFlowPhase switched = new EnergyFlowPhase();
        switched.sample(0, false);
        close(switched.sample(30, false), 3.5);
        close(switched.sample(30, true), 3.5); // switch does not jump the phase
        close(switched.sample(30 + 20.0 / 7.0, true), 4.5);
        close(switched.sample(30 + 20.0 / 7.0, true), 4.5); // repeated render pass

        EnergyFlowPhase[] many = new EnergyFlowPhase[100];
        for (int i = 0; i < many.length; i++) {
            many[i] = new EnergyFlowPhase();
            many[i].sample(0, i % 2 == 0);
        }
        for (int i = 0; i < many.length; i++) {
            close(many[i].sample(i % 2 == 0 ? 20 : 60, i % 2 == 0), 0);
        }
        System.out.println("PASS: seven-frame order, 60/20 tick cycle durations, seamless state transitions, and independent clocks.");
    }
}
