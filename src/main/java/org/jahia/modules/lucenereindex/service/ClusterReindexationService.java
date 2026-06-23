package org.jahia.modules.lucenereindex.service;

import java.util.Timer;
import java.util.TimerTask;

import javax.jcr.RepositoryException;

import org.apache.commons.lang.reflect.MethodUtils;
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

    private Timer watchdog;

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
            watchdog = new Timer(true);
            watchdog.schedule(new TimerTask() {
                @Override
                public void run() {
                    performReindexCheck();
                }
            }, 0, scanInterval);
            logger.info("ClusterReindexationService activated with scan interval {}ms", scanInterval);
        } catch (RepositoryException ex) {
            logger.error("Indexer couldn't be initialized!", ex);
        }
    }

    @Deactivate
    public void deactivate() {
        if (watchdog != null) {
            watchdog.cancel();
            watchdog = null;
        }
        logger.info("ClusterReindexationService deactivated");
    }

    private void initializeJCRNode() throws RepositoryException {
        JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback() {
            @Override
            public Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
                if (!session.nodeExists(NODE_PATH)) {
                    JCRNodeWrapper settingsNode = session.getNode("/settings");
                    JCRNodeWrapper adminNode = settingsNode.addNode("reindexAdmin", "jnt:reindexAdminInfo");
                    adminNode.saveSession();
                }
                return null;
            }
        });
    }

    private void performReindexCheck() {
        logger.debug("Start Perform reindex check");
        try {
            JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback() {
                @Override
                public Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
                    try {
                        String currentId = (String) MethodUtils.invokeExactMethod(
                                reindexManager.getLocalClusterNode(), "getId", null);
                        JCRNodeWrapper admin = session.getNode(NODE_PATH);
                        if (admin.hasNodes()) {
                            for (JCRNodeWrapper node : admin.getNodes()) {
                                if (node.getProperty("nodeId").getString().equals(currentId)) {
                                    logger.info("Reindex started on node {}", currentId);
                                    node.remove();
                                    session.save();
                                    ((JahiaRepositoryImpl) ((SpringJackrabbitRepository) JCRSessionFactory
                                            .getInstance().getDefaultProvider().getRepository())
                                            .getRepository()).scheduleReindexing();
                                    return null;
                                }
                            }
                        }
                    } catch (Exception ex) {
                        logger.error("reindex perform has error", ex);
                    }
                    return null;
                }
            });
        } catch (RepositoryException ex) {
            logger.error("Check of reindexation failed", ex);
        }
    }
}
