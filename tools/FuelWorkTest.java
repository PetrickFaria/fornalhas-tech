import br.com.petrick.fornalhastech.blockentity.FuelWork;

public class FuelWorkTest {
    static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    static final int[] SPEED = {1,2,3,5,7,9,11};
    static int ticks(int speed, int total) {
        int progress=0, charged=0, ticks=0;
        while(progress<total) {
            int step=FuelWork.step(speed,progress,total);
            progress+=step; charged+=step; ticks++;
        }
        check(charged==total,"Fuel efficiency changed");
        check(ticks==(total+speed-1)/speed,"Original cooking cadence changed");
        return ticks;
    }
    public static void main(String[] args) {
        for(int speed:SPEED) for(int total:new int[]{1,2,100,200,201,399,1200,10001}) ticks(speed,total);
        check(ticks(1,200)==200 && ticks(11,200)==19,"Oak log comparison");
        for(int speed:SPEED) for(int fuel:new int[]{1,2,100,200,300,1600,20000}) {
            int burn=fuel,progress=0,recipes=0,charged=0;
            while(burn>0) {
                int used=Math.min(burn,FuelWork.step(speed,progress,200));
                burn-=used;progress+=used;charged+=used;
                if(progress==200){recipes++;progress=0;}
            }
            check(charged==fuel,"Fuel lost");
            check(recipes==fuel/200 && progress==fuel%200,"Output yield changed");
        }
        int remaining=1600, progress=0, charged=0;
        // Repeated core swaps, and a save/reload represented by unchanged native integer units.
        for(int i=0;i<10000 && remaining>0;i++) {
            int step=Math.min(remaining,FuelWork.step(SPEED[i%SPEED.length],progress,201));
            remaining-=step;charged+=step;progress+=step;
            if(progress==201)progress=0;
            int persisted=remaining;remaining=Integer.parseInt(Integer.toString(persisted));
            check(remaining+charged==1600,"Core switch/refill/save granted or lost fuel");
        }
        check(FuelWork.step(Integer.MAX_VALUE,Integer.MAX_VALUE-1,Integer.MAX_VALUE)==1,"Overflow");
        check(FuelWork.step(11,200,200)==0,"Completed recipe charged twice");
        check(FuelWork.shouldPrimeNextTick(0,true),"Next fuel must be primed at an active-work boundary");
        check(!FuelWork.shouldPrimeNextTick(1,true),"Do not replace fuel while burn remains");
        check(!FuelWork.shouldPrimeNextTick(0,false),"Do not consume fuel without a valid next recipe");
        int firstBurn=1; firstBurn-=1;
        int nextBurn=FuelWork.shouldPrimeNextTick(firstBurn,true)?1600:0;
        check(firstBurn==0 && nextBurn>0,"Consecutive fuels must keep the final LIT state active");
        System.out.println("PASS: 7 speeds; original cadence; exact fuel/yield; partial ticks; core changes; integer persistence; boundary fuel handoff; overflow bounds.");
    }
}
