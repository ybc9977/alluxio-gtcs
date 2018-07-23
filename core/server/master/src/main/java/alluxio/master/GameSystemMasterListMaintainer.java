package alluxio.master;

import alluxio.AlluxioURI;
import alluxio.collections.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *  created by byangak on 30/06/2018
 */

public final class GameSystemMasterListMaintainer {

    private final static Logger LOG = LoggerFactory.getLogger(GameSystemMasterListMaintainer.class);

    private static Map<String,List<String>> cacheMap = new HashMap<>();

    /** FileList which is unsettled during the querying process */
    private static Map<AlluxioURI,Boolean> fileList = new HashMap<>();

    /** FileList which is settled and always representing the realistic circumstance */
    private static Map<AlluxioURI,Boolean> cacheList = new HashMap<>();

    /** Each user is a Pair contains userId(Long) and isCachingOptionChanged(Boolean) */
    private static ArrayList<Pair<String,Boolean>> userList = new ArrayList<>();

    public static void addfile(String path){
        AlluxioURI uri = new AlluxioURI(path);
        fileList.put(uri,false);
        cacheList.put(uri,false);
        LOG.info("a file has been added successfully into " + path );
    }

    public static void deletefile(String path) {
        AlluxioURI uri = new AlluxioURI(path);
        if(fileList.containsKey(uri)){
            fileList.remove(uri);
        }else{
            LOG.info("File not found");
        }
    }

    public static void adduser(String userId) {
        Pair<String,Boolean> p1 = new Pair<>(userId, true);
        Pair<String,Boolean> p2 = new Pair<>(userId, false);
        if(!userList.contains(p1)&&!userList.contains(p2)){
            Pair<String, Boolean> pair = new Pair<>(userId, false);
            userList.add(pair);
            cacheMap.put(userId,null);
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

    public static ArrayList<Pair<String,Boolean>> getUserList(){
        return userList;
    }

    public static Map<String,List<String>> getCacheMap(){
        return cacheMap;
    }

    public static void setCacheMap(Map<String,List<String>> mCacheMap){
        cacheMap = mCacheMap;
        LOG.info(String.valueOf("cacheMap: "+cacheMap));
//        boolean isEqual = true;
//        LOG.info(String.valueOf("cacheMap: "+cacheMap));
//        for (String user:cacheMap.keySet()){
//            if(cacheMap.get(user)!=null){
//                for (String file:mCacheMap.get(user)){
//                    if(!cacheMap.get(user).contains(file)){
//                        isEqual = false;
//                    }
//                }
//            }else{
//                isEqual = false;
//            }
//        }
//        if(!isEqual){
//            cacheMap = mCacheMap;
//            LOG.info(String.valueOf("cacheMap: "+cacheMap));
//        }else{
//            LOG.info("cacheMap stay the same");
//        }
    }

    public static Map<String,Boolean> getFileList(){
        Map<String,Boolean> fileMap = new HashMap<>();
        for(AlluxioURI key : fileList.keySet()){
            fileMap.put(key.toString(), fileList.get(key));
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
        LOG.info("fileList: "+String.valueOf(files));
        for(String file:files.keySet()){
            AlluxioURI uri = new AlluxioURI(file);
            if (files.get(file)!= fileList.get(uri)){
                fileList.replace(uri,files.get(file));
            }
        }
    }

    public static void setCacheList(Map<String,Boolean> files){
        LOG.info("cacheList: "+String.valueOf(files));
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
