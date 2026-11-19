package org.jahia.modules.lucenereindex.action;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyBoolean;
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

import org.jahia.bin.ActionResult;
import org.jahia.modules.lucenereindex.flow.ReindexManager;
import org.junit.Before;
import org.junit.Test;

public class ReindexActionTest {

    private ReindexManager reindexManager;
    private ReindexAction action;

    @Before
    public void setUp() throws Exception {
        reindexManager = mock(ReindexManager.class);
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

    private Map<String, List<String>> params(String actionValue, String checkConsistency) {
        Map<String, List<String>> parameters = params(actionValue);
        parameters.put("checkConsistency", Collections.singletonList(checkConsistency));
        return parameters;
    }

    private ActionResult execute(Map<String, List<String>> parameters) {
        // Target the package-private dispatch directly. The authorization and CSRF gates in
        // doExecute belong to integration scope and are not exercised by these unit tests.
        return action.handleAction(parameters);
    }

    @Test
    public void addReindexWithNodeId_callsPutNodeForReindexAndReturns200() throws Exception {
        ActionResult result = execute(params("addreindex:node123"));

        verify(reindexManager, times(1)).putNodeForReindex("node123", false);
        assertEquals(200, result.getResultCode());
    }

    @Test
    public void addReindexWithEmptyNodeId_returnsBadRequestAndDoesNotCallManager() throws Exception {
        ActionResult result = execute(params("addreindex:"));

        verify(reindexManager, never()).putNodeForReindex(anyString(), anyBoolean());
        assertEquals(ActionResult.BAD_REQUEST.getResultCode(), result.getResultCode());
    }

    @Test
    public void addReindexWithWhitespaceNodeId_returnsBadRequestAndDoesNotCallManager() throws Exception {
        ActionResult result = execute(params("addreindex:   "));

        verify(reindexManager, never()).putNodeForReindex(anyString(), anyBoolean());
        assertEquals(ActionResult.BAD_REQUEST.getResultCode(), result.getResultCode());
    }

    @Test
    public void reindexFull_callsReindexFullAndReturns200() throws Exception {
        ActionResult result = execute(params("reindexfull"));

        verify(reindexManager, times(1)).reindexFull(false);
        assertEquals(200, result.getResultCode());
    }

    @Test
    public void missingActionParameter_returnsBadRequest() throws Exception {
        ActionResult result = execute(params(null));

        verify(reindexManager, never()).putNodeForReindex(anyString(), anyBoolean());
        verify(reindexManager, never()).reindexFull(anyBoolean());
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

        verify(reindexManager, never()).putNodeForReindex(anyString(), anyBoolean());
        verify(reindexManager, never()).reindexFull(anyBoolean());
        assertEquals(ActionResult.BAD_REQUEST.getResultCode(), result.getResultCode());
    }

    @Test
    public void addReindexWithCheckConsistencyTrue_requestsConsistencyCheck() throws Exception {
        ActionResult result = execute(params("addreindex:node123", "true"));

        verify(reindexManager, times(1)).putNodeForReindex("node123", true);
        assertEquals(200, result.getResultCode());
    }

    @Test
    public void reindexFullWithCheckConsistencyTrue_requestsConsistencyCheck() throws Exception {
        ActionResult result = execute(params("reindexfull", "true"));

        verify(reindexManager, times(1)).reindexFull(true);
        assertEquals(200, result.getResultCode());
    }

    @Test
    public void checkConsistencyOtherThanTrue_doesNotRequestConsistencyCheck() throws Exception {
        execute(params("addreindex:node123", "yes"));

        verify(reindexManager, times(1)).putNodeForReindex("node123", false);
    }

    @Test
    public void checkConsistencyIsCaseInsensitive() throws Exception {
        execute(params("reindexfull", "TRUE"));

        verify(reindexManager, times(1)).reindexFull(true);
    }

    @Test
    public void reindexFullWithCheckConsistencyFalse_doesNotRequestConsistencyCheck() throws Exception {
        execute(params("reindexfull", "false"));

        verify(reindexManager, times(1)).reindexFull(false);
    }
}
