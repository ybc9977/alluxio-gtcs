package alluxio.master;

import alluxio.collections.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 *  created by byangak on 30/06/2018
 */

public final class GameMasterListMaintainer{

    private final static Logger LOG = LoggerFactory.getLogger(GameMasterListMaintainer.class);

    private static Map<Long,Boolean> fileList =new HashMap<>();

    private static ArrayList<Pair<Long,Boolean>> userList =new ArrayList<>();

    public static void addfile(Long fileId, boolean isCached){
        fileList.put(fileId, isCached);
    }

    public static void adduser(Long userId) {
        Pair<Long, Boolean> pair = new Pair<>(userId, true);
        userList.add(pair);
    }

    public static void deleteuser(Long userId) {
        Pair<Long, Boolean> pair1 = new Pair<>(userId, false);
        Pair<Long, Boolean> pair2 = new Pair<>(userId, true);
        if(userList.contains(pair1)){
            userList.remove(pair1);
        }else if(userList.contains(pair2)){
            userList.remove(pair2);
        }else{
            LOG.info("user not found");
        }
    }

    public static void deletefile(Long fileId) {
        if(fileList.containsKey(fileId)){
            fileList.remove(fileId);
        }else{
            LOG.info("File not found");
        }
    }

    public static Map<Long,Boolean> getfile(){
        return fileList;
    }

    public static ArrayList<Pair<Long,Boolean>> getuser(){
        return userList;
    }

    public static void changefile(final Map<Long,Boolean> list){
        fileList = list;
    }

    private GameMasterListMaintainer(){
        //prevent instantiation
    }

}
