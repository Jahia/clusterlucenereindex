package org.jahia.modules.lucenereindex.flow;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.jahia.services.content.JCRNodeIteratorWrapper;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRPropertyWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for the dedup logic in {@link ReindexManager#addNodeForReindexIfAbsent},
 * the only non-static, non-cluster-bound business logic in the module. The JCR API is
 * interface-based so it mocks cleanly without any Jahia container initialisation.
 */
public class ReindexManagerTest {

    private static final String ADMIN_PATH = "/settings/reindexAdmin";
    private static final String NODE_TYPE = "jnt:nodeToReindex";
    private static final String PROP_NODE_ID = "nodeId";

    private ReindexManager manager;
    private JCRSessionWrapper session;
    private JCRNodeWrapper admin;

    @Before
    public void setUp() throws Exception {
        manager = new ReindexManager();
        session = mock(JCRSessionWrapper.class);
        admin = mock(JCRNodeWrapper.class);
        when(session.getNode(ADMIN_PATH)).thenReturn(admin);
    }

    // Built as a standalone local (never inside a when(...).thenReturn(...)) to avoid
    // Mockito's nested-stubbing (UnfinishedStubbingException) pitfall.
    private JCRNodeIteratorWrapper children(JCRNodeWrapper... nodes) {
        JCRNodeIteratorWrapper iterator = mock(JCRNodeIteratorWrapper.class);
        when(iterator.iterator()).thenReturn(Arrays.asList(nodes).iterator());
        return iterator;
    }

    private JCRNodeWrapper existingEntry(String nodeId) throws Exception {
        JCRNodeWrapper node = mock(JCRNodeWrapper.class);
        JCRPropertyWrapper prop = mock(JCRPropertyWrapper.class);
        when(prop.getString()).thenReturn(nodeId);
        when(node.getProperty(PROP_NODE_ID)).thenReturn(prop);
        return node;
    }

    @Test
    public void addsEntryWhenQueueIsEmpty() throws Exception {
        JCRNodeIteratorWrapper empty = children();
        when(admin.getNodes()).thenReturn(empty);
        JCRNodeWrapper created = mock(JCRNodeWrapper.class);
        when(admin.addNode(anyString(), eq(NODE_TYPE))).thenReturn(created);

        manager.addNodeForReindexIfAbsent(session, "node-a");

        verify(admin).addNode(anyString(), eq(NODE_TYPE));
        verify(created).setProperty(PROP_NODE_ID, "node-a");
        verify(created).saveSession();
    }

    @Test
    public void skipsWhenSameNodeIdAlreadyQueued() throws Exception {
        JCRNodeWrapper existing = existingEntry("node-a");
        JCRNodeIteratorWrapper queued = children(existing);
        when(admin.getNodes()).thenReturn(queued);

        manager.addNodeForReindexIfAbsent(session, "node-a");

        verify(admin, never()).addNode(anyString(), anyString());
    }

    @Test
    public void addsWhenOnlyADifferentNodeIdIsQueued() throws Exception {
        JCRNodeWrapper other = existingEntry("node-x");
        JCRNodeIteratorWrapper queued = children(other);
        when(admin.getNodes()).thenReturn(queued);
        JCRNodeWrapper created = mock(JCRNodeWrapper.class);
        when(admin.addNode(anyString(), eq(NODE_TYPE))).thenReturn(created);

        manager.addNodeForReindexIfAbsent(session, "node-a");

        verify(admin).addNode(anyString(), eq(NODE_TYPE));
        verify(created).setProperty(PROP_NODE_ID, "node-a");
    }
}
