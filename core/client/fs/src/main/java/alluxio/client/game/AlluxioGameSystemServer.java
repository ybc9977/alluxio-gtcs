package alluxio.client.game;

import alluxio.Constants;
import alluxio.ProcessUtils;
import alluxio.PropertyKey;
import alluxio.RuntimeConstants;
import alluxio.util.CommonUtils;
import alluxio.util.ConfigurationUtils;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * created by byangak 27/07/18
 *
 * Entry point for the Game System Server
 *
 */

@ThreadSafe
public final class AlluxioGameSystemServer {
    private static final Logger LOG = LoggerFactory.getLogger(AlluxioGameSystemServer.class);
    private static String mUserId;

    /**
     * Starts the Alluxio Game System Server in certain node
     *
     * @param args command line arguments, should be only one with userID
     */
    public static void main(String[] args){

        if (args.length != 0) {
            LOG.info("java -cp {} {}", RuntimeConstants.ALLUXIO_JAR,
                    AlluxioGameSystemServer.class.getCanonicalName());
            System.exit(-1);
        }

        if (!ConfigurationUtils.masterHostConfigured()) {
            ProcessUtils.fatalError(LOG,
                    "Cannot run alluxio game system client; master hostname is not "
                            + "configured. Please modify %s to either set %s or configure zookeeper with "
                            + "%s=true and %s=[comma-separated zookeeper master addresses]",
                    Constants.SITE_PROPERTIES, PropertyKey.MASTER_HOSTNAME.toString(),
                    PropertyKey.ZOOKEEPER_ENABLED.toString(), PropertyKey.ZOOKEEPER_ADDRESS.toString());
        }

        CommonUtils.PROCESS_TYPE.set(CommonUtils.ProcessType.CLIENT);
        GameSystemServerProcess process = new GameSystemServerProcess();

        mUserId = process.getUserId();

        LOG.info("user "+mUserId+" has started");
        ProcessUtils.run(process);
        LOG.info("user "+mUserId+" has stopped");
    }

    private AlluxioGameSystemServer() {} // prevent instantiation
}
