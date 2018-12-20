package alluxio.client.game;

import alluxio.AlluxioURI;
import alluxio.Configuration;
import alluxio.PropertyKey;
import alluxio.Server;
import alluxio.client.ReadType;
import alluxio.client.file.BaseFileSystem;
import alluxio.client.file.FileInStream;
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
import java.util.concurrent.*;

/**
 *  created by byangak on 28/06/2018
 */

public class GameSystemServer extends BaseFileSystem implements Server<ClientNetAddress> {
    private static final Logger LOG = LoggerFactory.getLogger(GameSystemServer.class);


    private String mUserId;
    public ClientNetAddress mAddress;
    private BaseFileSystem mFileSystem;

    private List<Double> mPref;
    private int[] mSortedIndices;

    private List<Integer> mCachedFileIds; // the files that are cached by this client (in the game)
    private static String AlluxioFolder = "/test";

    private boolean shuffle=true;

    private File clientLog;

    private double mAccessRate;
    private int mAccessNum;

    /**
     * Constructs a new base file system.
     *
     * @param context file system context
     */
    public GameSystemServer(FileSystemContext context, String userId, BaseFileSystem fileSystem) throws IOException {
        super(context);
        mUserId = userId;
        mFileSystem = fileSystem;
        String currentDirectory = System.getProperty("user.dir");
        clientLog = new File(currentDirectory+'/'+mUserId+".txt");
        if (!clientLog.exists())
            clientLog.createNewFile();

        mAccessNum = Configuration.getInt(PropertyKey.ACCESS_COUNT);
        mAccessRate= Configuration.getDouble(PropertyKey.ACCESS_RATE);

    }

    public String getUserId(){
        return mUserId;
    }

//    private void setPrefList(Map<String,Boolean> fileList){
//        if (list.size()!=fileList.size()){
//            int i = 0;
//            for (String file : fileList.keySet()){
//                list.remove(file);
//                list.add(i,file);
//                i++;
//            }
//            Collections.shuffle(list);
//        }
//        if (shuffle){
//            Collections.shuffle(list);
//            ZipfDistribution zd = new ZipfDistribution(fileList.size(),1.05);
//            int count = 1;
//            for (String path : list) {
//                pref.put(path, zd.probability(count));
//                count++;
//            }
//            pref = sortByValue(pref);
//            prefList= new ArrayList<>(pref.keySet());
//            shuffle = false;
//        }
//    }

    Pair access(List<Double> prefs, String mode, List<Double> cachedRatio, List<Double> accessFactor){


        //for(String file : pref.keySet()) {
          //  interval.put(file, new ExponentialDistribution(pref.get(file)).sample()*1000);
        //}
        // initialize the random distribution to draw file Ids to acess
        RandomNumberGenerator rand = new RandomNumberGenerator();
        for(int i=0;i<prefs.size();i++){
            double pref = prefs.get(i);
            rand.addNumber(i, pref);
        }

        List<Future<Long>> timeList = new ArrayList<>(); // Record the latency of each read
        ExecutorService executorService = Executors.newCachedThreadPool();
        double hit = 0;
        for (int i=0;i<mAccessNum;i++){

            int fileId = rand.getNext(); // get a file Id to access given the pref distribution
            try {
                AlluxioURI uri = new AlluxioURI(AlluxioFolder+"/"+fileId);
                OpenFileOptions options = OpenFileOptions.defaults().setReadType(ReadType.NO_CACHE);
                FileInStream is = mFileSystem.openFile(uri,options);
                double thisHit = cachedRatio.get(fileId)* accessFactor.get(fileId);
                Future<Long> future = executorService.submit(new FileAccessThread(is, thisHit)); // run the file read in another thread
                hit += thisHit;
                timeList.add(future);

                // Access interval
                Double interval  = new ExponentialDistribution(1.0/mAccessRate).sample();
                System.out.println("Sleep" + interval.longValue()*1000 + "ms");
                Thread.sleep(interval.longValue()*1000);
            } catch (InterruptedException | IOException | AlluxioException e) {
                e.printStackTrace();
            }
            break;


        }
        // Now get the read latencies.
        long t = 0L;
        try{
            FileOutputStream fop = new FileOutputStream(clientLog,true);
            OutputStreamWriter writer = new OutputStreamWriter(fop);
            writer.write("\n"+mode+":\n");


            for (Future<Long> futureRuntime :timeList){
                Long runtime =futureRuntime.get();
                writer.write("" + runtime + "\t");
                t += runtime;
            }
            writer.write("\n");
            writer.flush();
            writer.close();
            fop.close();
        }catch(InterruptedException | ExecutionException | IOException e){
            e.printStackTrace();
        }
        return new Pair<>(hit/mAccessNum,t);
    }
//
//    Pair access(Map<String, Double> pref, List<Double> factor) throws IOException {
//        FileOutputStream fop = new FileOutputStream(clientLog,true);
//        OutputStreamWriter writer = new OutputStreamWriter(fop);
//        writer.write("\nFairRide:\n");
//        Map<String, Double> interval = new HashMap<>();
//        ArrayList<Pair<String,Long>> timeList = new ArrayList<>();
//        for(String file : pref.keySet()) {
//            interval.put(file, new ExponentialDistribution(pref.get(file)).sample()*1000);
//        }
//        double hit = 0;
//        for (int i = 0;i<accessNum;i++){
//            int count = 0, goal = (int)(new Random(System.nanoTime()).nextDouble()*pref.size());
//            for (String file:pref.keySet()) {
//                if(count==goal){
//                    try {
//                        long start = System.currentTimeMillis();
//                        OpenFileOptions options = OpenFileOptions.defaults().setReadType(ReadType.NO_CACHE);
//                        FileInStream is =mFileSystem.openFile(new AlluxioURI(file),options);
//                        byte [] fileBuf = new byte[(int) (is.remaining()+is.getPos())];
//                        is.read(fileBuf);
//                        is.close();
//                        timeList.add(new Pair<>(file,System.currentTimeMillis()-start));
//                        if(mFileSystem.getStatus(new AlluxioURI(file)).getInAlluxioPercentage()!=0) {
//                            hit+=factor.get(count);
//                        }
//                        Thread.sleep(interval.get(file).longValue());
//                    } catch (InterruptedException | AlluxioException | IOException e) {
//                        e.printStackTrace();
//                    }
//                    break;
//                }
//                count++;
//            }
//        }
//        writer.write('[');
//        for (Pair<String, Long> p :timeList){
//            writer.write('('+p.getFirst().substring(6)+','+p.getSecond()+')');
//        }
//        writer.write(']');
//        writer.flush();
//        writer.close();
//        fop.close();
//        long t = 0L;
//        for (Pair<String, Long> aTimeList : timeList) {
//            t += aTimeList.getSecond();
//        }
//        return new Pair<>((hit/accessNum),t);
//    }

//
//    /**
//     *
//     * Check whether cache allocation need to change and how to
//     *
//     * @param fileList this time's fileList needed to be cached
//     * @return cacheList or null
//     */
//
//     ArrayList<String> checkCacheChange(Map<String,Boolean> fileList, int QUOTA) {
//
//        ArrayList<String> cachingList = new ArrayList<>();
//        setPrefList(fileList);
//        for(String path: prefList){
//            if (QUOTA >0 && !fileList.get(path)){
//                QUOTA--;
//                cachingList.add(path);
//                fileList.replace(path,true);
//            }else if (QUOTA <= 0){
//                return cachingList;
//            }
//        }
//        return cachingList;
//    }

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

    //public Map<String, Double> getPref() {
      //   return pref;
    //}

    public void reset() {
         shuffle = true;
    }
}
