package alluxio.client.game;

import alluxio.AlluxioURI;
import alluxio.Server;
import alluxio.client.ReadType;
import alluxio.client.file.BaseFileSystem;
import alluxio.client.file.FileSystemContext;
import alluxio.client.file.options.OpenFileOptions;
import alluxio.collections.Pair;
import alluxio.exception.AlluxioException;
import alluxio.thrift.ClientNetAddress;
import alluxio.thrift.GameSystemCacheService;
import com.google.common.base.Preconditions;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.ZipfDistribution;
import org.apache.thrift.TProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/**
 *  created by byangak on 28/06/2018
 */

public class GameSystemServer extends BaseFileSystem implements Server<ClientNetAddress> {
    private static final Logger LOG = LoggerFactory.getLogger(GameSystemServer.class);

    private ArrayList<String> prefList;
    private String mUserId;
    public ClientNetAddress mAddress;
    private BaseFileSystem mFileSystem;
    private Map<String,Double> pref = new HashMap<>();
    private ArrayList<String> list = new ArrayList<>();
    private boolean shuffle=true;
    private int accessNum = 200;
    private File clientLog;


    /**
     * Constructs a new base file system.
     *
     * @param context file system context
     */
    public GameSystemServer(FileSystemContext context, String userId, BaseFileSystem fileSystem) throws IOException {
        super(context);
        mUserId = userId;
        mFileSystem = fileSystem;
        clientLog = new File("~/alluxio-gtcs/client"+mUserId+".txt");
        if (!clientLog.exists())
            clientLog.createNewFile();
    }

    public String getUserId(){
        return mUserId;
    }

    private void setPrefList(Map<String,Boolean> fileList){
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
                pref.put(path, zd.probability(count));
                count++;
            }
            pref = sortByValue(pref);
            prefList= new ArrayList<>(pref.keySet());
            shuffle = false;
        }
    }

    Pair accessFile(Map<String, Double> pref, String mode) throws IOException {
        FileOutputStream fop = new FileOutputStream(clientLog,true);
        OutputStreamWriter writer = new OutputStreamWriter(fop);
        writer.write(mode+":\n");
        Long time = System.currentTimeMillis();
        Map<String, Double> interval = new HashMap<>();
        ArrayList<Pair<String,Long>> timeList = new ArrayList<>();
        for(String file : pref.keySet()) {
            interval.put(file, new ExponentialDistribution(pref.get(file)).sample()*1000);
        }
        int hit = 0;
        for (int i=0;i<accessNum;i++){
            int count = 0, goal = (int)(new Random(System.nanoTime()).nextDouble()*pref.size());
            for (String file:pref.keySet()) {
                if(count==goal){
                    try {
                        long start = System.currentTimeMillis();
                        AlluxioURI uri = new AlluxioURI(file);
                        OpenFileOptions options = OpenFileOptions.defaults().setReadType(ReadType.NO_CACHE);
                        mFileSystem.openFile(uri,options);
                        timeList.add(new Pair<>(file,System.currentTimeMillis()-start));
                        if(mFileSystem.getStatus(uri).getInAlluxioPercentage()!=0) {
                            hit++;
                        }
                        Thread.sleep(interval.get(file).longValue());
                    } catch (InterruptedException | AlluxioException | IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
                count++;
            }
        }
        writer.write(String.valueOf(timeList));
        writer.flush();
        writer.close();
        fop.close();
        return new Pair<>(((double)hit/(double)accessNum),System.currentTimeMillis()-time);
    }

    Pair access(Map<String, Double> pref, List<Double> factor) throws IOException {
        FileOutputStream fop = new FileOutputStream(clientLog,true);
        OutputStreamWriter writer = new OutputStreamWriter(fop);
        writer.write("FairRide:\n");
        Long time = System.currentTimeMillis();
        Map<String, Double> interval = new HashMap<>();
        ArrayList<Pair<String,Long>> timeList = new ArrayList<>();
        for(String file : pref.keySet()) {
            interval.put(file, new ExponentialDistribution(pref.get(file)).sample()*1000);
        }
        double hit = 0;
        for (int i = 0;i<accessNum;i++){
            int count = 0, goal = (int)(new Random(System.nanoTime()).nextDouble()*pref.size());
            for (String file:pref.keySet()) {
                if(count==goal){
                    try {
                        long start = System.currentTimeMillis();
                        OpenFileOptions options = OpenFileOptions.defaults().setReadType(ReadType.NO_CACHE);
                        mFileSystem.openFile(new AlluxioURI(file),options);
                        timeList.add(new Pair<>(file,System.currentTimeMillis()-start));
                        if(mFileSystem.getStatus(new AlluxioURI(file)).getInAlluxioPercentage()!=0) {
                            hit+=factor.get(count);
                        }
                        Thread.sleep(interval.get(file).longValue());
                    } catch (InterruptedException | AlluxioException | IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
                count++;
            }
        }
        writer.write(String.valueOf(timeList));
        writer.flush();
        writer.close();
        fop.close();
        return new Pair<>((hit/accessNum),System.currentTimeMillis()-time);
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

    private void printLog(Object o) throws IOException {
    }

    /**
     *
     * Check whether cache allocation need to change and how to
     *
     * @param fileList this time's fileList needed to be cached
     * @return cacheList or null
     */

     ArrayList<String> checkCacheChange(Map<String,Boolean> fileList, int QUOTA) {

        ArrayList<String> cachingList = new ArrayList<>();
        setPrefList(fileList);
        for(String path: prefList){
            if (QUOTA >0 && !fileList.get(path)){
                QUOTA--;
                cachingList.add(path);
                fileList.replace(path,true);
            }else if (QUOTA <= 0){
                return cachingList;
            }
        }
        return cachingList;
    }

    @Override
    public Set<Class<? extends Server>> getDependencies() {
        return new HashSet<>();
    }

    @Override
    public String getName() {
        return "Client "+mUserId;
    }

    @Override
    public Map<String, TProcessor> getServices() {
        Map<String, TProcessor> services = new HashMap<>();
        services.put("Client "+mUserId,
                new GameSystemCacheService.Processor<>(
                        new GameSystemServiceHandler(this)));
        return services;
    }

    @Override
    public void start(ClientNetAddress address) {
         mAddress = address;
         Preconditions.checkNotNull(mAddress, "mAddress");
         LOG.info("Starting client server to reply for Master's require" );
    }


    @Override
    public void stop() {
        LOG.info("Stopping client server to reply for Master's require");
    }

    public Map<String, Double> getPref() {
         return pref;
    }

    public void reset() {
         shuffle = true;
    }
}
