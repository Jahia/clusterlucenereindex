package org.jahia.modules.lucenereindex.service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.JahiaRepositoryImpl;
import org.jahia.modules.lucenereindex.flow.ReindexManager;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.content.impl.jackrabbit.SpringJackrabbitRepository;
import org.jahia.settings.SettingsBean;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true)
@Designate(ocd = ClusterReindexationService.Config.class)
public class ClusterReindexationService {

    @ObjectClassDefinition(name = "Cluster Lucene Reindex - Scan Configuration")
    public @interface Config {
        @AttributeDefinition(name = "Scan interval (ms)", description = "Interval in milliseconds between reindex checks")
        long scanInterval() default 30000L;
    }

    private static final Logger logger = LoggerFactory.getLogger(ClusterReindexationService.class);

    private static final String NODE_PATH = "/settings/reindexAdmin";
    private static final String PROP_NODE_ID = "nodeId";

    private ScheduledExecutorService watchdog;

    @Reference
    private ReindexManager reindexManager;

    @Activate
    public void activate(Config config) {
        if (!SettingsBean.getInstance().isClusterActivated()) {
            logger.info("No Cluster active. {} not initialized.", getClass().getName());
            return;
        }
        try {
            initializeJCRNode();
            long scanInterval = config.scanInterval();
            watchdog = Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "cluster-lucene-reindex-watchdog");
                thread.setDaemon(true);
                return thread;
            });
            // Initial delay 0: run the first check immediately on activation (matching the
            // previous Timer behaviour), then every scanInterval ms thereafter.
            watchdog.scheduleWithFixedDelay(this::runReindexCheck, 0L, scanInterval, TimeUnit.MILLISECONDS);
            logger.info("ClusterReindexationService activated with scan interval {}ms", scanInterval);
        } catch (RepositoryException ex) {
            logger.error("Indexer couldn't be initialized!", ex);
        }
    }

    @Deactivate
    public void deactivate() {
        if (watchdog != null) {
            watchdog.shutdownNow();
            watchdog = null;
        }
        logger.info("ClusterReindexationService deactivated");
    }

    /**
     * Wraps the periodic check so that NO exception escapes to the executor.
     * A {@link ScheduledExecutorService} silently cancels all future runs of a
     * task that throws, so swallowing-and-logging here keeps the watchdog alive.
     */
    private void runReindexCheck() {
        try {
            performReindexCheck();
        } catch (Exception ex) { // NOSONAR - the watchdog must survive any single failed check
            logger.error("Reindex check failed", ex);
        }
    }

    private void initializeJCRNode() throws RepositoryException {
        JCRTemplate.getInstance().doExecuteWithSystemSession((JCRCallback<Object>) session -> {
            if (!session.nodeExists(NODE_PATH)) {
                JCRNodeWrapper settingsNode = session.getNode("/settings");
                JCRNodeWrapper adminNode = settingsNode.addNode("reindexAdmin", "jnt:reindexAdminInfo");
                adminNode.saveSession();
            }
            return null;
        });
    }

    private void performReindexCheck() throws RepositoryException {
        logger.debug("Start Perform reindex check");
        final String currentId = reindexManager.getLocalClusterId();
        if (currentId == null) {
            logger.debug("Local cluster node id unavailable, skipping reindex check");
            return;
        }
        Boolean scheduled = JCRTemplate.getInstance().doExecuteWithSystemSession((JCRCallback<Boolean>) session -> {
            // Read a fresh, cluster-replicated view of the queue before claiming.
            session.refresh(false);
            JCRNodeWrapper admin = session.getNode(NODE_PATH);
            for (JCRNodeWrapper node : admin.getNodes()) {
                if (node.getProperty(PROP_NODE_ID).getString().equals(currentId)) {
                    return claimAndReindex(session, node, currentId);
                }
            }
            return Boolean.FALSE;
        });
        if (Boolean.TRUE.equals(scheduled)) {
            logger.debug("Reindexation was scheduled for the current cluster node");
        }
    }

    private Boolean claimAndReindex(JCRSessionWrapper session, JCRNodeWrapper node, String currentId)
            throws RepositoryException {
        logger.info("Reindex started on node {}", currentId);
        // Schedule the local reindex FIRST and only drop the queue entry once it
        // has been triggered, so a failure here does not silently lose the request.
        scheduleLocalReindexing();
        node.remove();
        session.save();
        return Boolean.TRUE;
    }

    private void scheduleLocalReindexing() throws RepositoryException {
        Object repository = ((SpringJackrabbitRepository) JCRSessionFactory.getInstance()
                .getDefaultProvider().getRepository()).getRepository();
        if (repository instanceof JahiaRepositoryImpl) {
            ((JahiaRepositoryImpl) repository).scheduleReindexing();
        } else {
            logger.error("Unexpected repository implementation {} - cannot schedule reindexing",
                    repository != null ? repository.getClass().getName() : "null");
        }
    }
}
