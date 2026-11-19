package org.jahia.modules.lucenereindex.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.jackrabbit.core.JahiaRepositoryImpl;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRPropertyWrapper;
import org.junit.Test;

/**
 * Unit tests for the consistency-check handling of the watchdog: how the flag is read from a
 * queue entry and how it reaches {@link JahiaRepositoryImpl#scheduleReindexing(boolean)}.
 */
public class ClusterReindexationServiceTest {

    private static final String PROP_CHECK_CONSISTENCY = "checkConsistency";

    private JCRNodeWrapper entry(Boolean checkConsistency) throws Exception {
        JCRNodeWrapper node = mock(JCRNodeWrapper.class);
        when(node.hasProperty(PROP_CHECK_CONSISTENCY)).thenReturn(checkConsistency != null);
        if (checkConsistency != null) {
            JCRPropertyWrapper prop = mock(JCRPropertyWrapper.class);
            when(prop.getBoolean()).thenReturn(checkConsistency);
            when(node.getProperty(PROP_CHECK_CONSISTENCY)).thenReturn(prop);
        }
        return node;
    }

    @Test
    public void entryWithoutFlag_doesNotRequestConsistencyCheck() throws Exception {
        assertFalse(ClusterReindexationService.isConsistencyCheckRequested(entry(null)));
    }

    @Test
    public void entryWithFlagFalse_doesNotRequestConsistencyCheck() throws Exception {
        assertFalse(ClusterReindexationService.isConsistencyCheckRequested(entry(false)));
    }

    @Test
    public void entryWithFlagTrue_requestsConsistencyCheck() throws Exception {
        assertTrue(ClusterReindexationService.isConsistencyCheckRequested(entry(true)));
    }

    @Test
    public void schedulesReindexWithoutConsistencyCheck() throws Exception {
        JahiaRepositoryImpl repository = mock(JahiaRepositoryImpl.class);

        ClusterReindexationService.scheduleReindexing(repository, false);

        verify(repository).scheduleReindexing(false);
    }

    @Test
    public void schedulesReindexWithConsistencyCheck() throws Exception {
        JahiaRepositoryImpl repository = mock(JahiaRepositoryImpl.class);

        ClusterReindexationService.scheduleReindexing(repository, true);

        verify(repository).scheduleReindexing(true);
    }

    @Test
    public void unexpectedRepositoryType_isIgnored() throws Exception {
        // Must log and return instead of throwing ClassCastException.
        ClusterReindexationService.scheduleReindexing(new Object(), true);
        ClusterReindexationService.scheduleReindexing(null, false);
    }
}
