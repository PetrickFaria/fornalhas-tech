import br.com.petrick.fornalhastech.block.FurnaceLightLevel;
public class FurnaceLightLevelTest {
 public static void main(String[] args) {
  int[][] cases={{0,0,0},{0,1,5},{1,0,10},{1,1,15}};
  for (int[] c:cases) if(FurnaceLightLevel.get(c[0]!=0,c[1]!=0)!=c[2]) throw new AssertionError(java.util.Arrays.toString(c));
  System.out.println("PASS: core/burning light levels are 0, 5, 10, 15.");
 }
}
