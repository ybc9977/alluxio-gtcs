package alluxio.master.game;

import alluxio.exception.status.AlluxioStatusException;

/**
 * Created by yyuau on 20/12/2018.
 *
 * Update the client preferences
 *
 */
public class UpdatePreferences {

  public static void main(String[] argvs){
    try {
      GameSystemMaster.getPref();
    }catch(AlluxioStatusException e){
      e.printStackTrace();
    }

  }
}
