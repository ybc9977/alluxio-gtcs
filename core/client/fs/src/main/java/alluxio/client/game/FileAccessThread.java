package alluxio.client.game;

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

  public FileAccessThread(FileInStream fis){
    mFis=fis;
  }

  public Long call() {
    byte[] fileBuf = new byte[(int) (mFis.remaining() + mFis.getPos())];
    long start = System.currentTimeMillis();
    try {
      mFis.read(fileBuf);
      mFis.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return System.currentTimeMillis() - start;
  }

}
