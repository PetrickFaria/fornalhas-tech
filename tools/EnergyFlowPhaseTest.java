import br.com.petrick.fornalhastech.client.EnergyFlowPhase;
public class EnergyFlowPhaseTest {
    static void close(double actual, double expected) {
        if (Math.abs(actual - expected) > 1e-8) throw new AssertionError(actual + " != " + expected);
    }
    public static void main(String[] args) {
        EnergyFlowPhase idle = new EnergyFlowPhase(), active = new EnergyFlowPhase();
        idle.sample(0, false); active.sample(0, true);
        close(idle.sample(12, false), 1); close(active.sample(12, true), 3);
        close(idle.sample(60, false), 0); close(active.sample(20, true), 0);
        EnergyFlowPhase switched = new EnergyFlowPhase();
        switched.sample(0, false); close(switched.sample(18, false), 1.5);
        close(switched.sample(18, true), 1.5); close(switched.sample(22, true), 2.5);
        close(switched.sample(22, false), 2.5); close(switched.sample(34, false), 3.5);
        close(switched.sample(34, false), 3.5); // multiple render passes do not advance time
        close(switched.sample(52, false), 0); // seamless five-to-zero wrap
        EnergyFlowPhase[] many = new EnergyFlowPhase[100];
        for(int i=0;i<100;i++){ many[i]=new EnergyFlowPhase();many[i].sample(0,i%2==0); }
        for(int i=0;i<100;i++)close(many[i].sample(12,i%2==0),i%2==0?3:1);
        System.out.println("PASS: 12/4 ticks per frame, 60/20 tick cycles, phase continuity, repeated render passes, 100 independent clocks.");
    }
}
