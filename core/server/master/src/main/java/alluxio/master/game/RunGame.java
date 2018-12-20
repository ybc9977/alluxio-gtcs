package alluxio.master.game;

/**
 * Created by yyuau on 20/12/2018.
 *
 * Start game (after user registration) from scripts.
 *
 */
public class RunGame {

  public static void main(String[] argvs){
    int fileNumber = Integer.parseInt(argvs[0]);
    int quota = Integer.parseInt(argvs[1]);
    GameSystemMaster.runAll(fileNumber,quota);
  }
}
