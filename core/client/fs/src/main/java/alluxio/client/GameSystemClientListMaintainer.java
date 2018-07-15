package alluxio.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 *  created by byangak on 01/07/2018
 */

public final class GameSystemClientListMaintainer {

    private final static Logger LOG = LoggerFactory.getLogger(GameSystemClientListMaintainer.class);

    private static ArrayList<String> prefList = new ArrayList<>();

    private static Map<String,Double> pref = new HashMap<>();

    private static ArrayList<String> cacheList = new ArrayList<>();

    private static Long userId = 0L;

    public GameSystemClientListMaintainer(){}

    public void setUserId(Long user){
        userId = user;
    }
    public Long getUserId(){
        return userId;
    }

    public ArrayList<String> getPrefList(Map<String,Boolean> fileList){
        double p = 1.0;
        for(String path: fileList.keySet()){
            double p_now = p * Math.random();
            pref.put(path,p_now);
            p = p * (1-p_now);
            //prefList.set((int) (Math.random() * fileList.size()), path);
        }
        LOG.info("randomized preference list for user "+ userId);
        pref = sortByValue(pref);
        LOG.info(pref.toString());
        prefList.addAll(pref.keySet());
        return prefList;
    }

    static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        List<Map.Entry<K, V>> list = new ArrayList<>(map.entrySet());
        list.sort(Map.Entry.comparingByValue());

        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    public boolean changeCacheList(ArrayList<String> mCacheList){
        //if(cacheList==mCacheList){
        //    LOG.info("CacheList stay the same for user "+userId);
        //    return false;
        //}else{
        //    LOG.info("CacheList changed for user "+userId);
            cacheList=mCacheList;
            return true;
        //}
    }

    public ArrayList<String> getCacheList(){
        return cacheList;
    }

}
