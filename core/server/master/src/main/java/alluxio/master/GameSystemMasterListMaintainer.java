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

    private static Map<AlluxioURI,Boolean> fileListBefore = new HashMap<>();

    private static Map<AlluxioURI,Boolean> fileListAfter = new HashMap<>();

    private static ArrayList<Pair<Long,Boolean>> userList = new ArrayList<>();

    public static void addfile(String path, boolean isCached){
        AlluxioURI uri = new AlluxioURI(path);
        fileListBefore.put(uri, isCached);
        LOG.info("a file has been added successfully into " + path + "and cache " +isCached);
    }

    public static void setFileListBefore(Map<String,Boolean> fileList){
        for(String file:fileList.keySet()){
            AlluxioURI uri = new AlluxioURI(file);
            if (fileList.get(file)!=fileListBefore.get(uri)){
                fileListBefore.replace(uri,fileList.get(file));
            }
        }

    }

    public static void adduser(Long userId) {
        Pair<Long,Boolean> p1 = new Pair<>(userId, true);
        Pair<Long,Boolean> p2 = new Pair<>(userId, false);
        if(!userList.contains(p1)&&!userList.contains(p2)){
            Pair<Long, Boolean> pair = new Pair<>(userId, false);
            userList.add(pair);
            LOG.info("user "+ userId +" is added successfully into userList");
        }else{
            LOG.info("user" + userId +" is already in the userList");
        }

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
        if(fileListBefore.containsKey(path)){
            fileListBefore.remove(path);
        }else{
            LOG.info("File not found");
        }
    }

    public static Map<String,Boolean> getfilebefore(){
        Map<String,Boolean> fileMap = new HashMap<>();
        for(AlluxioURI key : fileListBefore.keySet()){
            fileMap.put(key.toString(),fileListBefore.get(key));
        }
        return fileMap;
    }
    public static Map<String,Boolean> getfileafter(){
        Map<String,Boolean> fileMap = new HashMap<>();
        for(AlluxioURI key : fileListAfter.keySet()){
            fileMap.put(key.toString(),fileListAfter.get(key));
        }
        return fileMap;
    }

    public static ArrayList<Pair<Long,Boolean>> getuser(){
        return userList;
    }

    public static void changefile(final Map<String,Boolean> map){
        LOG.info("fileListAfter changed");
        Map<AlluxioURI,Boolean> list = new HashMap<>();
        for(String path : map.keySet()){
            AlluxioURI uri = new AlluxioURI(path);
            list.put(uri,map.get(path));
        }
        fileListAfter = list;
    }

    public static void changeMode(final String path, final boolean isPersisted){
        AlluxioURI uri = new AlluxioURI(path);
        if(fileListBefore.get(uri)!=isPersisted){
            fileListBefore.replace(uri,isPersisted);
        }
    }

    private GameSystemMasterListMaintainer(){
        // prevent instantiation
    }

}
