package alluxio.master.game;

/**
 * Created by byangak on 04/01/2019.
 *
 * Outcome comparison on client preference update
 *
 */

public class UpdatePreferenceCompare {

    public static void main(String[] argvs){
        int fileNum = Integer.parseInt(argvs[0]);
        int quota = Integer.parseInt(argvs[1]);
        int updateNum = Integer.parseInt(argvs[2]);
        int loopNum = Integer.parseInt(argvs[3]);
        GameSystemMaster.updatePrefCompare(fileNum,quota,updateNum,loopNum);

    }

}
