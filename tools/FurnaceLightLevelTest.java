import br.com.petrick.fornalhastech.block.FurnaceLightLevel;
public class FurnaceLightLevelTest {
 public static void main(String[] args) {
  for (int appearance=0; appearance<=6; appearance++) {
   boolean colored=appearance>0;
   int off=FurnaceLightLevel.get(appearance,false);
   int on=FurnaceLightLevel.get(appearance,true);
   int expectedOff=colored?10:0, expectedOn=colored?15:10;
   if(off!=expectedOff || on!=expectedOn) throw new AssertionError("appearance="+appearance+" got "+off+"/"+on);
  }
  System.out.println("PASS: empty appearance emits 0/10; all six colored appearances emit 10/15.");
 }
}
