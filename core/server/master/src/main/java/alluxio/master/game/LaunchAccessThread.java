package alluxio.master.game;

import alluxio.collections.Pair;
import alluxio.exception.status.AlluxioStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Created by yyuau on 18/12/2018.
 */

public class LaunchAccessThread implements Callable {

  private GameSystemClient mClient;
  private String mMode; // the client needs this to log the current mode
  private List<Double> mCachedRatio; // the client needs this to simulate disk I/O delay
  private List<Double> mAccessFactor; // the client needs this to simulate delay
  private int mAcessNumber;

  public LaunchAccessThread(GameSystemClient client, String mode, List<Double> cachedRatio, List<Double> accessFactor){
    mClient=client;
    mMode = mode;
    mCachedRatio = new ArrayList<Double>(cachedRatio); // copy
    mAccessFactor = new ArrayList<Double>(accessFactor); // copy
  }

  public Pair<Double, Long> call() throws AlluxioStatusException{
    return mClient.access(mMode,mCachedRatio,mAccessFactor);
  }

}