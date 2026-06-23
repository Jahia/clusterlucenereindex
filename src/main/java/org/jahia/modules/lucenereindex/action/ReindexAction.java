package org.jahia.modules.lucenereindex.action;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.modules.lucenereindex.flow.ReindexManager;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.jahia.services.usermanager.JahiaUser;
import org.jahia.services.usermanager.JahiaUserManagerService;
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
    }

    @Override
    public ActionResult doExecute(HttpServletRequest req,
                                  RenderContext renderContext,
                                  Resource resource,
                                  JCRSessionWrapper session,
                                  Map<String, List<String>> parameters,
                                  URLResolver urlResolver) throws Exception {

        JahiaUser user = renderContext.getUser();
        if (JahiaUserManagerService.isGuest(user) || !user.isAdminMember(null)) {
            logger.warn("Unauthorized clusterReindex attempt by user: {}",
                    user != null ? user.getUsername() : "anonymous");
            return new ActionResult(HttpServletResponse.SC_FORBIDDEN, null, new JSONObject());
        }

        String csrfParam = req.getParameter("csrfToken");
        String csrfSession = (String) req.getSession().getAttribute("clusterReindex.csrf");
        if (csrfParam == null || csrfSession == null || !csrfParam.equals(csrfSession)) {
            logger.warn("CSRF token mismatch for clusterReindex");
            return new ActionResult(HttpServletResponse.SC_FORBIDDEN, null, new JSONObject());
        }

        List<String> actionValues = parameters.get("action");
        if (actionValues == null || actionValues.isEmpty()) {
            logger.warn("ReindexAction called without 'action' parameter");
            return ActionResult.BAD_REQUEST;
        }

        String action = actionValues.get(0);

        if (action.startsWith("addreindex:")) {
            String nodeId = action.substring("addreindex:".length());
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
