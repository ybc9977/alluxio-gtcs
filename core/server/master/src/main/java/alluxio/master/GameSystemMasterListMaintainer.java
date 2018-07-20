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

    /** FileList which is unsettled during the querying process */
    private static Map<AlluxioURI,Boolean> fileList = new HashMap<>();

    /** FileList which is settled and always representing the realistic circumstance */
    private static Map<AlluxioURI,Boolean> cacheList = new HashMap<>();

    /** Each user is a Pair contains userId(Long) and isCachingOptionChanged(Boolean) */
    private static ArrayList<Pair<Long,Boolean>> userList = new ArrayList<>();

    public static void addfile(String path, boolean isCached){
        AlluxioURI uri = new AlluxioURI(path);
        fileList.put(uri, isCached);
        cacheList.put(uri, isCached);
        LOG.info("a file has been added successfully into " + path + ", cache status: " +isCached);
    }

    public static void deletefile(String path) {
        if(fileList.containsKey(path)){
            fileList.remove(path);
        }else{
            LOG.info("File not found");
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
            LOG.info("user "+ userId +" is already in the userList");
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
            LOG.info("user "+ userId+ " not found");
        }
    }

    public static ArrayList<Pair<Long,Boolean>> getUserList(){
        return userList;
    }

    public static Map<String,Boolean> getFileList(){
        Map<String,Boolean> fileMap = new HashMap<>();
        for(AlluxioURI key : fileList.keySet()){
            fileMap.put(key.toString(),fileList.get(key));
        }
        return fileMap;
    }
    public static Map<String,Boolean> getCacheList(){
        Map<String,Boolean> fileMap = new HashMap<>();
        for(AlluxioURI key : cacheList.keySet()){
            fileMap.put(key.toString(), cacheList.get(key));
        }
        return fileMap;
    }

    public static void setFileList(Map<String,Boolean> files){
        LOG.info("fileList changed");
        for(String file:files.keySet()){
            AlluxioURI uri = new AlluxioURI(file);
            if (files.get(file)!=fileList.get(uri)){
                fileList.replace(uri,files.get(file));
            }
        }
    }

    public static void setCacheList(Map<String,Boolean> files){
        LOG.info("cacheList changed");
        for(String file:files.keySet()){
            AlluxioURI uri = new AlluxioURI(file);
            if (files.get(file)!=cacheList.get(uri)){
                cacheList.replace(uri,files.get(file));
            }
        }
    }

    public static void changeFileMode(final String path, final boolean isPersisted){
        AlluxioURI uri = new AlluxioURI(path);
        if(cacheList.get(uri)!=isPersisted){
            cacheList.replace(uri,isPersisted);
            fileList.replace(uri,isPersisted);
        }
    }

    private GameSystemMasterListMaintainer(){
        // prevent instantiation
    }

}
