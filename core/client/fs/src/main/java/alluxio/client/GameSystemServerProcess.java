package alluxio.client;

import alluxio.*;
import alluxio.Process;
import alluxio.client.file.BaseFileSystem;
import alluxio.client.file.FileSystemContext;
import alluxio.client.file.GameSystemServer;
import alluxio.exception.AlluxioException;
import alluxio.metrics.MetricsSystem;
import alluxio.network.thrift.BootstrapServerTransport;
import alluxio.network.thrift.ThriftUtils;
import alluxio.security.authentication.TransportProvider;
import alluxio.thrift.ClientNetAddress;
import alluxio.util.CommonUtils;
import alluxio.util.WaitForOptions;
import alluxio.util.network.NetworkAddressUtils;
import com.google.common.base.Function;
import com.google.common.base.Throwables;
import net.jcip.annotations.NotThreadSafe;
import org.apache.thrift.TMultiplexedProcessor;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TTransportFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;

/**
 * created by byangak at 27/07/18
 *
 * This class is to set up game system server
 */
@NotThreadSafe
public class GameSystemServerProcess implements Process {
    private static final Logger LOG = LoggerFactory.getLogger(GameSystemServerProcess.class);

    /** The GameSystemServer Registry. */
    private Registry<GameSystemServer, ClientNetAddress> mRegistry;

    /** The transport provider to create thrift server transport. */
    private TransportProvider mTransportProvider;

    /** Thread pool for thrift. */
    private TThreadPoolServer mThriftServer;

    /** Server socket for thrift. */
    private TServerSocket mThriftServerSocket;

    /** The address for the rpc server. */
    private InetSocketAddress mRpcAddress;

    /** Worker start time in milliseconds. */
    private long mStartTimeMs;

    /** The corresponding GameSystemServer. */
    private GameSystemServer mGameSystemServer;

    private FileSystemContext context = FileSystemContext.create(null);

    private BaseFileSystem mFileSystem = BaseFileSystem.get(context);

    private String mUserId;

    /**
     * Creates a new instance of {@link GameSystemServerProcess}.
     */
    GameSystemServerProcess(){

        mUserId = context.mAppId.substring(4);

        mGameSystemServer =  new GameSystemServer(context,mUserId);

        try {
            mRegistry = new Registry<>();
            mRegistry.add(GameSystemServer.class, mGameSystemServer);
            mStartTimeMs = System.currentTimeMillis();
            mTransportProvider = TransportProvider.Factory.create();
            mThriftServerSocket = new TServerSocket(
                    NetworkAddressUtils.getBindAddress(NetworkAddressUtils.ServiceType.CLIENT_RPC));
            int rpcPort = ThriftUtils.getThriftPort(mThriftServerSocket);
            String rpcHost = ThriftUtils.getThriftSocket(mThriftServerSocket).getInetAddress()
                    .getHostAddress();
            // reset master rpc port
            mRpcAddress = new InetSocketAddress(rpcHost, rpcPort);
            mThriftServer = createThriftServer();
            registerWithMaster(mUserId,rpcHost,getAddress());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private void registerWithMaster(String mUserId, String hostname, ClientNetAddress address) throws IOException, AlluxioException {
        mFileSystem.register(mUserId,hostname,address);
    }

    private TThreadPoolServer createThriftServer() {
        int minWorkerThreads = Configuration.getInt(PropertyKey.WORKER_BLOCK_THREADS_MIN);
        int maxWorkerThreads = Configuration.getInt(PropertyKey.WORKER_BLOCK_THREADS_MAX);
        TMultiplexedProcessor processor = new TMultiplexedProcessor();

        for (GameSystemServer client:mRegistry.getServers()) {
            registerServices(processor,client.getServices());
        }

        // Return a TTransportFactory based on the authentication type
        TTransportFactory transportFactory;
        try {
            String serverName = NetworkAddressUtils.getConnectHost(NetworkAddressUtils.ServiceType.CLIENT_RPC);
            transportFactory = new BootstrapServerTransport.Factory(mTransportProvider
                    .getServerTransportFactory(serverName));
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
        TThreadPoolServer.Args args = new TThreadPoolServer.Args(mThriftServerSocket)
                .minWorkerThreads(minWorkerThreads).maxWorkerThreads(maxWorkerThreads).processor(processor)
                .transportFactory(transportFactory)
                .protocolFactory(new TBinaryProtocol.Factory(true, true));

        if (Configuration.getBoolean(PropertyKey.TEST_MODE)) {
            args.stopTimeoutVal = 0;
        } else {
            args.stopTimeoutVal = Constants.THRIFT_STOP_TIMEOUT_SECONDS;
        }
        return new TThreadPoolServer(args);
    }


    private void registerServices(TMultiplexedProcessor processor, Map<String, TProcessor> services) {
        for (Map.Entry<String, TProcessor> service : services.entrySet()) {
            processor.registerProcessor(service.getKey(), service.getValue());
        }
    }

    public long getStartTimeMs() {
        return mStartTimeMs;
    }

    public long getUptimeMs() {
        return System.currentTimeMillis() - mStartTimeMs;
    }

    public InetSocketAddress getRpcAddress() {
        return mRpcAddress;
    }

    @Override
    public void start() throws Exception {
        startClients();
        LOG.info("Started {} with id {}", this, mRegistry.get(GameSystemServer.class).getUserId());


        LOG.info("Alluxio GameSystemServer version {} started. "
                        + "bindHost={}, connectHost={}, rpcPort={}",
                RuntimeConstants.VERSION,
                NetworkAddressUtils.getBindHost(NetworkAddressUtils.ServiceType.CLIENT_RPC),
                NetworkAddressUtils.getConnectHost(NetworkAddressUtils.ServiceType.CLIENT_RPC),
                NetworkAddressUtils.getPort(NetworkAddressUtils.ServiceType.CLIENT_RPC));
        mThriftServer.serve();
        LOG.info("Alluxio GameSystemServer ended");
    }

    private void startClients() throws IOException {
        MetricsSystem.startSinks();
        mRegistry.start(getAddress());
    }

    @Override
    public void stop() throws Exception {
        stopServing();
        stopClients();
        LOG.info("Alluxio GameSystemServer ended");
    }

    private void stopServing() {
        mThriftServer.stop();
        mThriftServerSocket.close();
        MetricsSystem.stopSinks();
    }

    private void stopClients() throws IOException {
        mRegistry.stop();
    }

    @Override
    public boolean waitForReady(int timeoutMs) {
        return CommonUtils.waitFor(this + " to start", new Function<Void, Boolean>() {
            @Override
            public Boolean apply(Void input) {
                return mThriftServer.isServing() && mRegistry.get(GameSystemServer.class).getUserId() != null;
            }
        }, WaitForOptions.defaults().setTimeoutMs(timeoutMs).setThrowOnTimeout(false));
    }

    public ClientNetAddress getAddress() {
        return new ClientNetAddress()
                .setHost(NetworkAddressUtils.getConnectHost(NetworkAddressUtils.ServiceType.CLIENT_RPC))
                .setRpcPort(mRpcAddress.getPort());
    }

    public String getUserId(){
        return mUserId;
    }
}

