package org.jahia.modules.lucenereindex.action;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.modules.lucenereindex.flow.ReindexManager;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = Action.class)
public class ReindexAction extends Action {

    private static final Logger logger = LoggerFactory.getLogger(ReindexAction.class);

    @Reference
    private ReindexManager reindexManager;

    @Activate
    public void activate() {
        setName("clusterReindex");
        // Triggering a (cluster-wide) Lucene reindex is a heavy, privileged
        // operation. Gate the action with the same server-administration
        // permission that guards the settings UI, so the Jahia dispatcher denies
        // any non-admin caller (the UI-side checks only hide the button).
        setRequiredPermission("adminUsers");
    }

    @Override
    public ActionResult doExecute(HttpServletRequest req,
                                  RenderContext renderContext,
                                  Resource resource,
                                  JCRSessionWrapper session,
                                  Map<String, List<String>> parameters,
                                  URLResolver urlResolver) throws Exception {

        List<String> actionValues = parameters.get("action");
        if (actionValues == null || actionValues.isEmpty()) {
            logger.warn("ReindexAction called without 'action' parameter");
            return ActionResult.BAD_REQUEST;
        }

        String action = actionValues.get(0);

        if (action.startsWith("addreindex:")) {
            String nodeId = action.substring("addreindex:".length()).trim();
            if (nodeId.isEmpty()) {
                logger.warn("ReindexAction: addreindex received empty nodeId");
                return ActionResult.BAD_REQUEST;
            }
            reindexManager.putNodeForReindex(nodeId);
        } else if ("reindexfull".equals(action)) {
            reindexManager.reindexFull();
        } else {
            logger.warn("ReindexAction: unknown action value '{}'", action);
            return ActionResult.BAD_REQUEST;
        }

        return new ActionResult(200, null, new JSONObject());
    }
}
