package alluxio.client.game;

import alluxio.Configuration;
import alluxio.PropertyKey;
import alluxio.client.file.FileInStream;

import java.io.IOException;
import java.util.concurrent.Callable;

/**
 * Created by yyuau on 16/12/2018.
 *
 * The thread to read a file.
 *
 */
public class FileAccessThread implements Callable {
  private FileInStream mFis;
  private double mHit;
  private long mMissPenalty;

  public FileAccessThread(FileInStream fis, double hit){
    mFis=fis;
    mHit=hit;
    mMissPenalty = Configuration.getLong(PropertyKey.MISS_PENALTY);
  }

  public Long call() {
    byte[] fileBuf = new byte[(int) (mFis.remaining() + mFis.getPos())];
    long start = System.currentTimeMillis();
    try {
      mFis.read(fileBuf); //todo: use a smaller fixed size buffer to avoid heap out-of-memory
      mFis.close();
      // Simulate cache miss delay
      System.out.println(String.format("Blocking delay %s ms", (long)(1000*(1-mHit))));
      Thread.sleep((long)(1000*(1-mHit)));
    } catch (IOException |InterruptedException e) {
      e.printStackTrace();
    }

    return System.currentTimeMillis() - start;
  }

}
