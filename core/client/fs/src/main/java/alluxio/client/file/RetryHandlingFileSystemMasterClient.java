/*
 * The Alluxio Open Foundation licenses this work under the Apache License, version 2.0
 * (the "License"). You may not use this work except in compliance with the License, which is
 * available at www.apache.org/licenses/LICENSE-2.0
 *
 * This software is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied, as more fully set forth in the License.
 *
 * See the NOTICE file distributed with this work for information regarding copyright ownership.
 */

package alluxio.client.file;

import alluxio.AbstractMasterClient;
import alluxio.AlluxioURI;
import alluxio.Constants;
import alluxio.client.file.options.*;
import alluxio.exception.status.AlluxioStatusException;
import alluxio.master.MasterClientConfig;
import alluxio.thrift.*;
import alluxio.wire.FileInfo;
import alluxio.wire.MountPointInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.concurrent.ThreadSafe;

/**
 * A wrapper for the thrift client to interact with the file system master, used by alluxio clients.
 *
 * Since thrift clients are not thread safe, this class is a wrapper to provide thread safety, and
 * to provide retries.
 */
@ThreadSafe
public final class RetryHandlingFileSystemMasterClient extends AbstractMasterClient
    implements FileSystemMasterClient {
  private FileSystemMasterClientService.Client mClient = null;

  /**
   * Creates a new {@link RetryHandlingFileSystemMasterClient} instance.
   *
   * @param conf master client configuration
   */
  public RetryHandlingFileSystemMasterClient(MasterClientConfig conf) {
    super(conf);
  }

  @Override
  protected AlluxioService.Client getClient() {
    return mClient;
  }

  @Override
  protected String getServiceName() {
    return Constants.FILE_SYSTEM_MASTER_CLIENT_SERVICE_NAME;
  }

  @Override
  protected long getServiceVersion() {
    return Constants.FILE_SYSTEM_MASTER_CLIENT_SERVICE_VERSION;
  }

  @Override
  protected void afterConnect() {
    mClient = new FileSystemMasterClientService.Client(mProtocol);
  }

  @Override
  public synchronized List<AlluxioURI> checkConsistency(final AlluxioURI path,
      final CheckConsistencyOptions options) throws AlluxioStatusException {
    return retryRPC(() -> {
      List<String> inconsistentPaths =
          mClient.checkConsistency(path.getPath(), options.toThrift()).getInconsistentPaths();
      List<AlluxioURI> inconsistentUris = new ArrayList<>(inconsistentPaths.size());
      for (String inconsistentPath : inconsistentPaths) {
        inconsistentUris.add(new AlluxioURI(inconsistentPath));
      }
      return inconsistentUris;
    }, "CheckConsistency");
  }

  @Override
  public synchronized void createDirectory(final AlluxioURI path,
      final CreateDirectoryOptions options) throws AlluxioStatusException {
    retryRPC(() -> mClient.createDirectory(path.getPath(), options.toThrift()), "CreateDirectory");
  }

  @Override
  public synchronized void createFile(final AlluxioURI path, final CreateFileOptions options)
      throws AlluxioStatusException {
    retryRPC(() -> mClient.createFile(path.getPath(), options.toThrift()), "CreateFile");
  }

  @Override
  public synchronized void completeFile(final AlluxioURI path, final CompleteFileOptions options)
      throws AlluxioStatusException {
    retryRPC(() -> mClient.completeFile(path.getPath(), options.toThrift()), "CompleteFile");
  }

  @Override
  public synchronized void delete(final AlluxioURI path, final DeleteOptions options)
      throws AlluxioStatusException {
    retryRPC(() -> mClient.remove(path.getPath(), options.isRecursive(), options.toThrift()),
        "Delete");
  }

  @Override
  public synchronized void free(final AlluxioURI path, final FreeOptions options)
      throws AlluxioStatusException {
    retryRPC(() -> mClient.free(path.getPath(), options.isRecursive(), options.toThrift()), "Free");
  }

  @Override
  public synchronized URIStatus getStatus(final AlluxioURI path, final GetStatusOptions options)
      throws AlluxioStatusException {
    return retryRPC(() -> new URIStatus(FileInfo.fromThrift(mClient.getStatus(path.getPath(),
        options.toThrift()).getFileInfo())), "GetStatus");
  }

  @Override
  public synchronized long getNewBlockIdForFile(final AlluxioURI path)
      throws AlluxioStatusException {
    return retryRPC(() -> mClient.getNewBlockIdForFile(path.getPath(),
        new GetNewBlockIdForFileTOptions()).getId(), "GetNewBlockIdForFile");
  }

  @Override
  public synchronized Map<String, alluxio.wire.MountPointInfo> getMountTable()
      throws AlluxioStatusException {
    return retryRPC(() -> {
      GetMountTableTResponse result = mClient.getMountTable();
      Map<String, alluxio.thrift.MountPointInfo> mountTableThrift = result.getMountTable();
      Map<String, alluxio.wire.MountPointInfo> mountTableWire = new HashMap<>();
      for (Map.Entry<String, alluxio.thrift.MountPointInfo> entry : mountTableThrift.entrySet()) {
        alluxio.thrift.MountPointInfo mMountPointInfoThrift = entry.getValue();
        alluxio.wire.MountPointInfo mMountPointInfoWire =
            MountPointInfo.fromThrift(mMountPointInfoThrift);
        mountTableWire.put(entry.getKey(), mMountPointInfoWire);
      }
      return mountTableWire;
    }, "GetMountTable");
  }

  @Override
  public void registerUser(String userId, String hostname, ClientNetAddress address, RegisterUserOptions options) throws AlluxioStatusException {
    retryRPC(() -> mClient.registerUser(userId, hostname, address, options.toThrift()),"RegisterUser");
  }

  @Override
  public synchronized List<URIStatus> listStatus(final AlluxioURI path,
      final ListStatusOptions options) throws AlluxioStatusException {
    return retryRPC(() -> {
      List<URIStatus> result = new ArrayList<>();
      for (alluxio.thrift.FileInfo fileInfo : mClient
          .listStatus(path.getPath(), options.toThrift()).getFileInfoList()) {
        result.add(new URIStatus(FileInfo.fromThrift(fileInfo)));
      }
      return result;
    }, "ListStatus");
  }

  @Override
  public synchronized void loadMetadata(final AlluxioURI path, final LoadMetadataOptions options)
      throws AlluxioStatusException {
    retryRPC(() -> mClient.loadMetadata(path.toString(), options.isRecursive(),
          new LoadMetadataTOptions()).getId(), "LoadMetadata");
  }

  @Override
  public synchronized void mount(final AlluxioURI alluxioPath, final AlluxioURI ufsPath,
      final MountOptions options) throws AlluxioStatusException {
    retryRPC(() -> mClient.mount(alluxioPath.toString(), ufsPath.toString(), options.toThrift()),
        "Mount");
  }

  @Override
  public synchronized void rename(final AlluxioURI src, final AlluxioURI dst)
      throws AlluxioStatusException {
    rename(src, dst, RenameOptions.defaults());
  }

  @Override
  public synchronized void rename(final AlluxioURI src, final AlluxioURI dst,
      final RenameOptions options) throws AlluxioStatusException {
    retryRPC(() -> mClient.rename(src.getPath(), dst.getPath(), options.toThrift()), "Rename");
  }

  @Override
  public synchronized void setAttribute(final AlluxioURI path, final SetAttributeOptions options)
      throws AlluxioStatusException {
    retryRPC(() -> mClient.setAttribute(path.getPath(), options.toThrift()), "SetAttribute");
  }

  @Override
  public synchronized void scheduleAsyncPersist(final AlluxioURI path)
      throws AlluxioStatusException {
    retryRPC(() -> mClient.scheduleAsyncPersistence(path.getPath(),
        new ScheduleAsyncPersistenceTOptions()), "ScheduleAsyncPersist");
  }

  @Override
  public synchronized void unmount(final AlluxioURI alluxioPath) throws AlluxioStatusException {
    retryRPC(() -> mClient.unmount(alluxioPath.toString(), new UnmountTOptions()), "Unmount");
  }

  @Override
  public synchronized void updateUfsMode(final AlluxioURI ufsUri,
      final UpdateUfsModeOptions options) throws AlluxioStatusException {
    retryRPC(() -> mClient.updateUfsMode(ufsUri.getRootPath(), options.toThrift()),
        "UpdateUfsMode");
  }

  @Override
  public synchronized void runGame(final int fileNumber, final int quota) throws AlluxioStatusException{
      retryRPC(() -> mClient.runGame(fileNumber, quota),
              "RunGame");
  }

  @Override
  public synchronized void updatePrefComp(final int fileNumber, final int quota, final int updateNum, final int loopNum) throws AlluxioStatusException{
    retryRPC(() -> mClient.updatePrefComp(fileNumber, quota, updateNum, loopNum),
            "UpdatePrefComp");
  }
}
