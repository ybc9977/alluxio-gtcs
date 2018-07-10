package alluxio.master;

import alluxio.AlluxioURI;
import alluxio.collections.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 *  created by byangak on 30/06/2018
 */

public final class GameSystemMasterListMaintainer {

    private final static Logger LOG = LoggerFactory.getLogger(GameSystemMasterListMaintainer.class);

    private static Map<AlluxioURI,Boolean> fileList = new HashMap<>();

    private static ArrayList<Pair<Long,Boolean>> userList = new ArrayList<>();

    public static void addfile(String path, boolean isCached){
        AlluxioURI uri = new AlluxioURI(path);
        fileList.put(uri, isCached);
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

    public static void deletefile(String path) {
        if(fileList.containsKey(path)){
            fileList.remove(path);
        }else{
            LOG.info("File not found");
        }
    }

    public static Map<String,Boolean> getfile(){
        Map<String,Boolean> fileMap = new HashMap<>();
        for(AlluxioURI key : fileList.keySet()){
            fileMap.put(key.toString(),fileList.get(key));
        }
        return fileMap;
    }

    public static ArrayList<Pair<Long,Boolean>> getuser(){
        return userList;
    }

    public static void changefile(final Map<String,Boolean> map){
        Map<AlluxioURI,Boolean> list = new HashMap<>();
        for(String path : map.keySet()){
            AlluxioURI uri = new AlluxioURI(path);
            list.put(uri,map.get(path));
        }
        fileList = list;
    }

    private GameSystemMasterListMaintainer(){
        // prevent instantiation
    }

}
