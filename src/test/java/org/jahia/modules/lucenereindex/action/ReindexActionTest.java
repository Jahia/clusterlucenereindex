package org.jahia.modules.lucenereindex.action;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.jahia.bin.ActionResult;
import org.jahia.modules.lucenereindex.flow.ReindexManager;
import org.junit.Before;
import org.junit.Test;

public class ReindexActionTest {

    private ReindexManager reindexManager;
    private HttpServletRequest request;
    private ReindexAction action;

    @Before
    public void setUp() throws Exception {
        reindexManager = mock(ReindexManager.class);
        request = mock(HttpServletRequest.class);
        action = new ReindexAction();
        injectReindexManager(action, reindexManager);
    }

    private void injectReindexManager(ReindexAction target, ReindexManager manager) throws Exception {
        Field field = ReindexAction.class.getDeclaredField("reindexManager");
        field.setAccessible(true);
        field.set(target, manager);
    }

    private Map<String, List<String>> params(String actionValue) {
        Map<String, List<String>> parameters = new HashMap<>();
        if (actionValue != null) {
            parameters.put("action", Collections.singletonList(actionValue));
        }
        return parameters;
    }

    private ActionResult execute(Map<String, List<String>> parameters) throws Exception {
        // The render context, resource, session and URL resolver arguments are not read by
        // the method under test, so null is passed to avoid heavy Jahia class initialisation.
        return action.doExecute(request, null, null, null, parameters, null);
    }

    @Test
    public void addReindexWithNodeId_callsPutNodeForReindexAndReturns200() throws Exception {
        ActionResult result = execute(params("addreindex:node123"));

        verify(reindexManager, times(1)).putNodeForReindex("node123");
        assertEquals(200, result.getResultCode());
    }

    @Test
    public void addReindexWithEmptyNodeId_returnsBadRequestAndDoesNotCallManager() throws Exception {
        ActionResult result = execute(params("addreindex:"));

        verify(reindexManager, never()).putNodeForReindex(anyString());
        assertEquals(ActionResult.BAD_REQUEST.getResultCode(), result.getResultCode());
    }

    @Test
    public void addReindexWithWhitespaceNodeId_returnsBadRequestAndDoesNotCallManager() throws Exception {
        ActionResult result = execute(params("addreindex:   "));

        verify(reindexManager, never()).putNodeForReindex(anyString());
        assertEquals(ActionResult.BAD_REQUEST.getResultCode(), result.getResultCode());
    }

    @Test
    public void reindexFull_callsReindexFullAndReturns200() throws Exception {
        ActionResult result = execute(params("reindexfull"));

        verify(reindexManager, times(1)).reindexFull();
        assertEquals(200, result.getResultCode());
    }

    @Test
    public void missingActionParameter_returnsBadRequest() throws Exception {
        ActionResult result = execute(params(null));

        verify(reindexManager, never()).putNodeForReindex(anyString());
        verify(reindexManager, never()).reindexFull();
        assertEquals(ActionResult.BAD_REQUEST.getResultCode(), result.getResultCode());
    }

    @Test
    public void emptyActionList_returnsBadRequest() throws Exception {
        Map<String, List<String>> parameters = new HashMap<>();
        parameters.put("action", Collections.<String>emptyList());

        ActionResult result = execute(parameters);

        assertEquals(ActionResult.BAD_REQUEST.getResultCode(), result.getResultCode());
    }

    @Test
    public void unknownAction_returnsBadRequest() throws Exception {
        ActionResult result = execute(params("somethingelse"));

        verify(reindexManager, never()).putNodeForReindex(anyString());
        verify(reindexManager, never()).reindexFull();
        assertEquals(ActionResult.BAD_REQUEST.getResultCode(), result.getResultCode());
    }
}
