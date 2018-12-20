package alluxio.client.game;


import alluxio.client.file.FileSystem;

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
    FileSystem fileSystem = FileSystem.Factory.get();
    fileSystem.runGame(fileNumber,quota);
  }
}
