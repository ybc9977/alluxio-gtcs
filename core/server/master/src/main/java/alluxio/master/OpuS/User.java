package alluxio.master.OpuS;

import alluxio.AlluxioURI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by yyuau on 11/7/2017.
 */
public class User {

  private static final Logger LOG = LoggerFactory.getLogger(User.class);
  public Integer id;
  // public Double effectiveHit; //  log it, instead of record it.
  Double accessfactor = 1.0; // for taxing of VCG mechanism
  Map<AlluxioURI, Long> accessMap = new HashMap<>();
  public List<AlluxioURI> mCachedList = new ArrayList<>(); // the cached files with an isolated cache, unware of the size
  public Map<AlluxioURI, Double> accessFactorForFairRide = new HashMap<>(); // the list for FairRide
  private FileWriter logWriter;
  public FileWriter factorWriter;

  public User(Integer newId) {
    // totalAccess = 0;
    id = newId;
    String currentDirectory = System.getProperty("user.dir");
    try  {
      File userLog=new File(currentDirectory + "/opus_logs/user_"+ id +".txt");
      logWriter = new FileWriter(userLog, true); // false for overriding; true for appending
      File factorLog=new File(currentDirectory + "/opus_logs/user_"+ id +"_factor.txt");
      factorWriter = new FileWriter(factorLog, true);
      logWriter.write("Start\n");
      factorWriter.write("Start\n");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public synchronized void access (AlluxioURI filePath, boolean isHit) throws IOException {
    if (accessMap.containsKey(filePath)) {
      Long count = accessMap.get(filePath);
      accessMap.put(filePath, count+1);
    }
    else
      accessMap.put(filePath, (long)1);

    // for logs
    if (isHit) {
      LOG.info("User " + id + ": cache hit when accessing " + filePath);
      // Long fileSize = OpuSMaster.fileSizeMap.get(filePath);
      Double cachedFration = OpuSMaster.fileLibrary.get(filePath);
      if (OpuSMaster.MODE.equals(OpuSMaster.NONE) || OpuSMaster.MODE.equals(OpuSMaster.ISOLATED) ||OpuSMaster.MODE.equals(OpuSMaster.OPUS_ISOLATED) )
        logWriter.write("1.0\t");
      else if (OpuSMaster.MODE.equals(OpuSMaster.FAIRRIDE)) { // For FairRide, the accessFactor has already consider the cached fraction.
        Double effectiveCacheHit = accessFactorForFairRide.containsKey(filePath)?accessFactorForFairRide.get(filePath):0.0;
        logWriter.write(effectiveCacheHit + "\t");
      }else { // OpuS
        Double effectiveCacheHit = cachedFration * accessfactor;
        logWriter.write(effectiveCacheHit + "\t");
      }
    }
    else {
      logWriter.write("0.0\t");
    }
    // printAccessMap();
  }

  public void printAccessMap() {
    System.out.print("Access Profile of User " + this.id);
    for (AlluxioURI key: accessMap.keySet()) {
      System.out.println(key + " " + accessMap.get(key));
    }
  }

  public synchronized void flushLog() throws IOException{
    logWriter.flush();
    factorWriter.flush();
  }
}
