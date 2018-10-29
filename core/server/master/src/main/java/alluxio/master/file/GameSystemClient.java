package alluxio.master.file;

import alluxio.AbstractClient;
import alluxio.AlluxioURI;
import alluxio.Constants;
import alluxio.collections.Pair;
import alluxio.exception.AccessControlException;
import alluxio.exception.FileDoesNotExistException;
import alluxio.exception.InvalidPathException;
import alluxio.exception.UnexpectedAlluxioException;
import alluxio.exception.status.AlluxioStatusException;
import alluxio.master.file.options.FreeOptions;
import alluxio.thrift.AlluxioService;
import alluxio.thrift.GameSystemCacheService;
import alluxio.thrift.GetPrefTOptions;
import alluxio.thrift.ResetTOptions;
import com.sun.org.apache.xpath.internal.operations.Bool;
import org.apache.commons.math3.distribution.PoissonDistribution;
import org.apache.commons.math3.distribution.ZipfDistribution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;

/**
 *  created by byangak on 30/07/2018
 *
 *  Game System Master side client class, one instance to communicate with certain Game System Server
 */

public class GameSystemClient extends AbstractClient {
    private static final Logger LOG = LoggerFactory.getLogger(GameSystemMaster.class);

    private GameSystemCacheService.Client mClient = null;

    private String mUserId;

    private Map<String, Double> mPref = new HashMap<>();

    private ArrayList<String> mPrefList = new ArrayList<>();

    private ArrayList<String> list = new ArrayList<>();

    private boolean shuffle = true;

    GameSystemClient(Subject subject, InetSocketAddress address, String userId) {
        super(subject, address);
        mUserId = userId;
    }

    @Override
    protected AlluxioService.Client getClient() {
        return mClient;
    }

    @Override
    protected String getServiceName() {
        return "Client "+mUserId;
    }

    @Override
    protected long getServiceVersion() {
        return Constants.GAME_SYSTEM_SERVICE_VERSION;
    }

    @Override
    protected void afterConnect() {
        mClient = new GameSystemCacheService.Client(mProtocol);
    }

    private synchronized void setPrefList (Map<String,Boolean> fileList){
        double start_time =System.currentTimeMillis();
        LOG.info("user " + mUserId + " previously shuffle? " + shuffle);
        if (list.size()!=fileList.size()){
            int i = 0;
            for (String file : fileList.keySet()){
                list.remove(file);
                list.add(i,file);
                i++;
            }
            Collections.shuffle(list);
        }
        if (shuffle){
            Collections.shuffle(list);
            ZipfDistribution zd = new ZipfDistribution(fileList.size(),1.05);
            int count = 1;
            for (String path : list) {
                mPref.put(path, zd.probability(count));
                count++;
            }
            mPref = sortByValue(mPref);
            mPrefList= new ArrayList<>(mPref.keySet());
            shuffle = false;
        }
        LOG.info("setPrefList time cost: "+ (System.currentTimeMillis()-start_time));
    }

    /** a remote procedure to call in client side server
     * @param fileList a map contains filePath & isCached */
    public synchronized List<String> checkCacheChange(Map<String, Boolean> fileList, int QUOTA) throws AlluxioStatusException {
        ArrayList<String> mCacheList = new ArrayList<>();
        setPrefList(fileList);
        for(String path: mPrefList){
            if (QUOTA >0 && !fileList.get(path)){
                QUOTA--;
                fileList.replace(path,true);
                mCacheList.add(path);
            }else if (QUOTA <= 0){
                return mCacheList;
            }
        }
        return mCacheList;
        //return retryRPC(() -> mClient.checkCacheChange(fileList).getCachingList(), "CheckCacheChange");
    }

    public Map<String,Double> getPref() throws AlluxioStatusException {
        return mPref;
        //return retryRPC(() -> mClient.getPref(new GetPrefTOptions()).getPref(), "GetPref");
    }

    public Map<String, Integer> access(Map<String,Double> prefList) throws AlluxioStatusException {
        Map<String,Integer> access = new HashMap<>();
        for(String file : prefList.keySet()){
            int acc = new PoissonDistribution(mPrefList.indexOf(file)+1).sample() * 10;
            access.put(file,acc);
        }
        return access;
        //return retryRPC(() -> mClient.access(prefList).getAccess(), "Access");
    }

    public void reset() throws AlluxioStatusException {
//        retryRPC(() -> mClient.reset(new ResetTOptions()), "Access");
        shuffle = true;
    }

    /**
     * Sort the Map by descending order according to the values
     */
    private <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {

        List<Map.Entry<K, V>> list = new ArrayList<>(map.entrySet());
        list.sort(Map.Entry.comparingByValue());
        for (int i = 0; i < Math.floor(list.size() / 2) ; i++){
            Map.Entry<K, V> temp;
            temp = list.get(i);
            list.set(i,list.get(list.size()-i-1));
            list.set(list.size()-i-1,temp);
        }
        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }


    public synchronized void cacheIt(Map<String,Boolean> fileList, Map<String,Boolean> cacheList, FileSystemMaster fsMaster){
        for (String file:fileList.keySet()){
            AlluxioURI uri = new AlluxioURI(file);
            if(fileList.get(file)!=cacheList.get(file)){
                if(fileList.get(file)){
                    try {
                        retryRPC(() -> mClient.load(file), "Load");
                        LOG.info("Load Process Complete, uri: " + uri.getPath());
                        cacheList.replace(file,true);
                    } catch (AlluxioStatusException e) {
                        e.printStackTrace();
                    }
                }else{
                    try {
                        fsMaster.free(uri,FreeOptions.defaults());
                        LOG.info("Free Process Complete, uri: " + uri.getPath());
                        cacheList.replace(file,false);
                    } catch (IOException | AccessControlException | FileDoesNotExistException | InvalidPathException | UnexpectedAlluxioException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
