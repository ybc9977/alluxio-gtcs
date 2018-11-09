package alluxio.master.OpuS;


import alluxio.AlluxioURI;
import alluxio.master.OpuS.UserFilePair;
import alluxio.PropertyKey;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import org.apache.commons.logging.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.util.*;

/**
 * Created by yyuau on 12/7/2017.
 */
public class OpuSMaster extends TimerTask{

  private static final Logger LOG = LoggerFactory.getLogger(OpuSMaster.class);
  private static final String CONFNAME = "opus.txt";
  public static final String NONE = "None"; //LRU
  public static final String ISOLATED = "Isolated";  // totally isolated, only ask for isolated allocation
  public static final String OPUS_SHARE = "OpuS"; // first stage of OpuS
  public static final String OPUS_ISOLATED = "OpuS_Isolated"; // second stage of OpuS, will try to get back to the first stage later
  public static final String FAIRRIDE = "FairRide";
  public static String MODE = NONE;  // Default no fair allocation
  public static int LINKED_HASH_MAP_INIT_CAPACITY = 5;

  public static Map<Integer, User> userMap = new HashMap<>();
  public static Map<AlluxioURI, Double> fileLibrary = new HashMap(); // cached fraction
  public static Map<AlluxioURI, Long> fileSizeMap = new HashMap(); // file size
  public static Map<UserFilePair<AlluxioURI, User>, Double> accessFactorMap = new HashMap<>();
  // public static Integer lastUserId = 0;
  private static Long totalAccess = (long)0;
  // todo if the input does't change, skip calling runOpuS

  private static Map<AlluxioURI, Boolean> mLRUCache; // to simulate the LRU cache, assuming equal file size.


  public OpuSMaster() {
    LOG.info("OpuS started");
    String currentDirectory = System.getProperty("user.dir");
    // File directory = new File(".");
    // currentDirectory = directory.getAbsolutePath();
    LOG.info("Current Directory:" + currentDirectory);
    try (BufferedReader br = new BufferedReader(new FileReader(currentDirectory + "/" + CONFNAME))) {

      MODE = br.readLine();
      LOG.info(MODE);
      LINKED_HASH_MAP_INIT_CAPACITY = Integer.parseInt(br.readLine()); // for use of LRU mode only
      LOG.info(" " + LINKED_HASH_MAP_INIT_CAPACITY);
    } catch (IOException e) {
      e.printStackTrace();
    }
    if(MODE.equals(OPUS_SHARE)) {
      LOG.info("Mode: OpuS");
    }
    else if (MODE.equals(FAIRRIDE)) {
      LOG.info("Mode: FairRide");
    }
    else if(MODE.equals(ISOLATED)){
      LOG.info("Mode: Isolated"); // in isolated cache, each user has a separate LRU cache
    }
    else if (MODE.equals(NONE)) {
      LOG.info("Mode: LRU");
      mLRUCache = Collections.synchronizedMap(new LinkedHashMap<AlluxioURI, Boolean>(LINKED_HASH_MAP_INIT_CAPACITY,
              0.75f, true));
      LOG.info("Total cache: " + LINKED_HASH_MAP_INIT_CAPACITY + " units.");
    }
  }

  public static void addUser(Integer id, User user) {
    userMap.put(id, user);
  }

  public static synchronized String access(Integer userId, AlluxioURI filePath, long fileSize) throws IOException {
    totalAccess += 1;
    LOG.info("Total access now: " + totalAccess);
    if (totalAccess % 20 == 0) // update it every 20 times of access
      runByAccessNumber();

    if (!userMap.containsKey(userId)) {
      LOG.info("Create user " + userId);
      User newUser = new User(userId);
      userMap.put(userId, newUser);
      logUserMap();
      // printUserMap();
    }
    User thisUser = userMap.get(userId);
    if(!fileLibrary.containsKey(filePath)){
      LOG.info("Add to library: " + filePath);
      fileLibrary.put(filePath, 0.0);
      fileSizeMap.put(filePath, fileSize);
      // printFileLibrary();
    }

    String returnMessage;
    boolean isHit = true;
    if (MODE.equals(NONE)) {
      if (mLRUCache.containsKey(filePath)) {
        LOG.info("Hit in LRU cache: " + filePath);
        mLRUCache.put(filePath, true); // true for applying an unused map value
        returnMessage =  "1.0:1.0";
      }
      else {
        LOG.info("Miss in LRU cache: " + filePath);
        isHit = false;
        if (mLRUCache.size() >= LINKED_HASH_MAP_INIT_CAPACITY)
          mLRUCache.remove(mLRUCache.keySet().iterator().next()); // remove the least recently accessed one
        mLRUCache.put(filePath, true); // true for applying an unused map value
        returnMessage =  "0.0:0.0";
      }
    }
    else if (MODE.equals(ISOLATED) || MODE.equals(OPUS_ISOLATED)) { // check the simulated isolated cache
        if (thisUser.mCachedList.contains(filePath)) {
          LOG.info("Hit in isolated cache: " + filePath);
          returnMessage =  "1.0:1.0";
        }
        else {
          LOG.info("Miss in isolated cache: " + filePath);
          isHit = false;
          returnMessage =  "0.0:0.0";
        }
    }
    else if (MODE.equals(FAIRRIDE)) {
      double accessFactor = thisUser.accessFactorForFairRide.containsKey(filePath)?thisUser.accessFactorForFairRide.get(filePath):0.0;
      returnMessage =  "1.0:" + accessFactor; // for FairRide, the accessfactor has already considered cached fraction!
    }
    else {
      // Now it must be MODE.equals(OPUS_SHARE)
      double accessFactor = thisUser.accessfactor;
      returnMessage =  fileLibrary.get(filePath) + ":" + accessFactor;
    }

    thisUser.access(filePath, isHit);
    return returnMessage;


  }

  public static void printUserMap() {
    for (Integer userId: userMap.keySet()) {
      System.out.println(userId + " " + userMap.get(userId));
    }
  }

  public static void printFileLibrary() {
    for (AlluxioURI filePath: fileLibrary.keySet()) {
      System.out.println(filePath);
    }
  }

  public static synchronized void logUserMap() {
    for (Integer userId: userMap.keySet()) {
      LOG.info(userId + " " + userMap.get(userId));
    }
  }

  public static synchronized void runOpuS()
  {
    if (MODE.equals(NONE)){
      LOG.info("No fair allocation"); // no need to run OpuS
      return;
    }

    ArrayList<String> cmd = new ArrayList<>();
    cmd.add("python");
    String currentDirectory = System.getProperty("user.dir");
    cmd.add(currentDirectory + "/python/OpuS.py");

    // specify what allocation to get
    cmd.add(MODE);

    // remember the order
    List<AlluxioURI> fileList = new ArrayList<>(fileLibrary.keySet());
    List<User> userList = new ArrayList<>(userMap.values());
    if (userList.size() <= 1) // if there is only one user, there is no need to do allocation
      return;

    // build the file size string
    StringBuilder sb = new StringBuilder();
    for (AlluxioURI file : fileList)
    {
      sb.append(fileSizeMap.get(file));
      sb.append(",");
    }
    String newString = sb.toString();
    newString = newString.substring(0, newString.length()-1); // remove the last comma
    cmd.add(newString);

    for(User user: userList) {
      //build the user access list string
      sb = new StringBuilder();
      for (AlluxioURI file : fileList)
      {
        long count= user.accessMap.containsKey(file)?user.accessMap.get(file):(long)0;
        sb.append(count + ",");
      }
      newString = sb.toString();
      newString = newString.substring(0, newString.length()-1); // remove the last comma
      cmd.add(newString);
    }

    LOG.info("cmd: " + cmd);
    // convert cmd to string[]
    Object[] objectList = cmd.toArray();
    String[] cmdArray =  Arrays.copyOf(objectList,objectList.length,String[].class);



    // Run Opus.py and parse the result
    String result = "";
    try {
      Process p = Runtime.getRuntime().exec(cmdArray);
      BufferedReader stdInput = new BufferedReader(new
              InputStreamReader(p.getInputStream()));
      result = stdInput.readLine();
      MODE = result;
      LOG.info("Received result for OpuS.py: " + MODE);
      if (result.equals(ISOLATED) || result.equals(OPUS_ISOLATED) ) {
        LOG.info("Get Isolated Allocation");
        for (User user: userList){
          user.mCachedList = new ArrayList<>(); // override
          String cachedListString = stdInput.readLine();
          LOG.info("cachedListString: " + cachedListString);
          String[] cachedList = cachedListString.split(",");
          for (String cachedindex: cachedList) {
            int index = Integer.parseInt(cachedindex);
            LOG.info("cache " + fileList.get(index) + "for user " + user.id);
            user.mCachedList.add(fileList.get(index));
          }
        }
      } else if (result.equals(FAIRRIDE)) {
        LOG.info("Get FairRide allocation");
        String cachedFractionString = stdInput.readLine();
        String[] cachedFractions = cachedFractionString.split(",");
        for(int i = 0; i<cachedFractions.length; i++) {
          Double cachedFraction = Double.parseDouble(cachedFractions[i]);
          fileLibrary.put(fileList.get(i), cachedFraction);
          LOG.info("file " + fileList.get(i) + " cached fraction : " + cachedFraction);
        }
        for (User user: userList){
          String accessFactorString = stdInput.readLine();
          LOG.info("Get access Factor String:" + accessFactorString);
          user.accessFactorForFairRide = new HashMap<>(); // override
          String[] accessFactors = accessFactorString.split(",");
          for(int i = 0; i<accessFactors.length; i++) {
            LOG.info("To parse access factor" + accessFactors[i]);
            Double accessFactor = Double.parseDouble(accessFactors[i]);
            user.accessFactorForFairRide.put(fileList.get(i), accessFactor);
            LOG.info("Access factor of user " + user.id + "for file " + fileList.get(i) + ": "+ accessFactor);
          }
        }
      } else if (result.equals(OPUS_SHARE)){ // OpuS
        LOG.info("Get Opus allocation");
        String cachedFractionString = stdInput.readLine();
        String accessFactorString = stdInput.readLine();
        String[] cachedFractions = cachedFractionString.split(",");
        String[] accessFactors = accessFactorString.split(",");
        for(int i = 0; i<cachedFractions.length; i++) {
          Double cachedFraction = Double.parseDouble(cachedFractions[i]);
          fileLibrary.put(fileList.get(i), cachedFraction);
          LOG.info("file " + fileList.get(i) + " cached fraction : " + cachedFraction);
        }
        for(int i = 0; i<accessFactors.length; i++) {
          Double accessFactor = Double.parseDouble(accessFactors[i]);
          userList.get(i).accessfactor=accessFactor;
          userList.get(i).factorWriter.write(accessFactor + "\t");
          LOG.info("user " + userList.get(i).id + " access factor : " + accessFactor);
        }
      } else {
        LOG.info("Result cannot be recognized!");
      }
      stdInput.close();
    } catch (Exception e ) { LOG.info("Wrong Message received: " + result);return;}
    LOG.info("runOpuS finishes");
  }

  public synchronized static void flushUserLogs(){

    for (User user: userMap.values()) {
      try {
        user.flushLog();
      } catch (IOException e) {
        LOG.info("Failed to flush the logs of user " + user.id);
      }
    }
  }
  public synchronized static void runByAccessNumber() {
    LOG.info("Start running OpuS triggered by access number");
    runOpuS();
    LOG.info("Flush the user logs");
    flushUserLogs();
  }

  public synchronized void run() {
    LOG.info("Start running OpuS");
    runOpuS();
    LOG.info("Flush the user logs");
    flushUserLogs();
  }
}
