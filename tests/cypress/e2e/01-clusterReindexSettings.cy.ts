describe('Cluster Lucene Reindex - settings page', () => {
    // Registered by registerExtensions.js as the adminRoute iframe target:
    // '/cms/adminframe/{workspace}/{lang}/settings.clusterlucenereindex.html'.
    // 'settings' is the fixed node name of Jahia's global settings node ('/settings'),
    // 'clusterlucenereindex' is the view name backed by globalSettings.clusterlucenereindex.jsp.
    const settingsPageUrl = '/cms/adminframe/default/en/settings.clusterlucenereindex.html';

    before(() => {
        cy.login();
    });

    it('renders the reindex page header for an admin', () => {
        cy.visit(settingsPageUrl);
        cy.contains('h2', 'Cluster Lucene Reindexation').should('be.visible');
    });

    it('reports the environment as non-clustered on a single-node test instance', () => {
        cy.visit(settingsPageUrl);

        // SettingsBean.isClusterActivated() is false on this single-node harness,
        // so ReindexManager.isCluster() is false and the JSP takes the non-cluster branch.
        cy.contains('Current environment is not a Cluster.').should('be.visible');

        // The reindex form (and the per-node reindex table) only render when isCluster is true.
        cy.get('#clusterReindexForm').should('not.exist');
    });
});
