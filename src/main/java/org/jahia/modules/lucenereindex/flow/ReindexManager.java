package org.jahia.modules.lucenereindex.flow;

import java.util.Set;
import java.util.UUID;

import javax.jcr.RepositoryException;

import org.apache.commons.lang.reflect.MethodUtils;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.settings.SettingsBean;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = ReindexManager.class)
public class ReindexManager {

    private static final Logger logger = LoggerFactory.getLogger(ReindexManager.class);

    private static final String CLUSTER_MANAGER_CLASS = "org.apache.karaf.cellar.core.ClusterManager";
    private static final String CELLAR_UNAVAILABLE =
            "Karaf Cellar ClusterManager is not available - is the cluster bundle started?";
    private static final String PROP_NODE_ID = "nodeId";
    private static final String PROP_CHECK_CONSISTENCY = "checkConsistency";
    private static final String REINDEX_ADMIN_PATH = "/settings/reindexAdmin";
    private static final String NODE_TYPE_TO_REINDEX = "jnt:nodeToReindex";

    public boolean isCluster() {
        return SettingsBean.getInstance().isClusterActivated();
    }

    private Object getClusterManager() {
        return BundleUtils.getOsgiService(CLUSTER_MANAGER_CLASS, null);
    }

    public Object getLocalClusterNode() {
        Object clusterManager = getClusterManager();
        if (clusterManager == null) {
            logger.warn(CELLAR_UNAVAILABLE);
            return null;
        }
        try {
            return MethodUtils.invokeExactMethod(clusterManager, "getNode", null);
        } catch (ReflectiveOperationException ex) {
            logger.error("Unable to resolve the local cluster node", ex);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public Set<Object> getClusterNodes() {
        Object clusterManager = getClusterManager();
        if (clusterManager == null) {
            logger.warn(CELLAR_UNAVAILABLE);
            return null;
        }
        try {
            Object result = MethodUtils.invokeExactMethod(clusterManager, "listNodes", null);
            return (result instanceof Set) ? (Set<Object>) result : null;
        } catch (ReflectiveOperationException ex) {
            logger.error("Unable to list cluster nodes", ex);
        }
        return null;
    }

    public String getLocalClusterId() {
        return getClusterId(getLocalClusterNode());
    }

    private String getClusterId(Object clusterNode) {
        if (clusterNode == null) {
            return null;
        }
        try {
            return (String) MethodUtils.invokeExactMethod(clusterNode, "getId", null);
        } catch (ReflectiveOperationException ex) {
            logger.error("Unable to resolve the cluster node id", ex);
        }
        return null;
    }

    /**
     * Queues a re-index of one cluster member.
     *
     * @param nodeId           the cluster member id
     * @param checkConsistency {@code true} to run a repository consistency check after the
     *                         re-index; it can take much longer than the re-index itself
     */
    public void putNodeForReindex(final String nodeId, final boolean checkConsistency) {
        logger.info("Add Node {} to reindexation (consistency check: {})", nodeId, checkConsistency);
        try {
            JCRTemplate.getInstance().doExecuteWithSystemSession((JCRCallback<Object>) session -> {
                addNodeForReindexIfAbsent(session, nodeId, checkConsistency);
                return null;
            });
        } catch (RepositoryException ex) {
            logger.error("Cannot add node to reindex list", ex);
        }
    }

    /**
     * Queues a re-index of every available cluster member.
     *
     * @param checkConsistency {@code true} to run a repository consistency check after each
     *                         re-index; it can take much longer than the re-index itself
     */
    public void reindexFull(final boolean checkConsistency) {
        logger.info("Reindex all available cluster members (consistency check: {})", checkConsistency);
        final Set<Object> nodes = getClusterNodes();
        if (nodes == null) {
            logger.warn("No cluster nodes found, skipping full reindex");
            return;
        }
        for (Object clusterNode : nodes) {
            final String nodeToReindex = getClusterId(clusterNode);
            if (nodeToReindex == null) {
                logger.warn("Skipping a cluster member whose id could not be resolved");
                continue;
            }
            try {
                JCRTemplate.getInstance().doExecuteWithSystemSession((JCRCallback<Object>) session -> {
                    addNodeForReindexIfAbsent(session, nodeToReindex, checkConsistency);
                    return null;
                });
            } catch (RepositoryException ex) {
                logger.error("Cannot add node to reindex list", ex);
            }
        }
    }

    /**
     * Adds a reindex queue entry for the given cluster node id, unless one already exists.
     * When an entry exists and a consistency check is requested, the check is turned on for
     * that entry, so the request is not lost. A request without a check never turns it off.
     * Package-private so the dedup logic can be unit-tested with a mocked JCR session.
     */
    void addNodeForReindexIfAbsent(JCRSessionWrapper session, String nodeId, boolean checkConsistency)
            throws RepositoryException {
        JCRNodeWrapper admin = session.getNode(REINDEX_ADMIN_PATH);
        for (JCRNodeWrapper node : admin.getNodes()) {
            if (node.getProperty(PROP_NODE_ID).getString().equals(nodeId)) {
                logger.info("Node already in list to reindex");
                if (checkConsistency) {
                    node.setProperty(PROP_CHECK_CONSISTENCY, true);
                    node.saveSession();
                    logger.info("Consistency check turned on for the queued reindex of node {}", nodeId);
                }
                return;
            }
        }
        // A UUID suffix avoids the name collision that System.currentTimeMillis()
        // could cause when two requests are queued within the same millisecond.
        JCRNodeWrapper indexNode = admin.addNode("reindex-" + UUID.randomUUID(), NODE_TYPE_TO_REINDEX);
        indexNode.setProperty(PROP_NODE_ID, nodeId);
        indexNode.setProperty(PROP_CHECK_CONSISTENCY, checkConsistency);
        indexNode.saveSession();
    }
}
