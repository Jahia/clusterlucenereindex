// ReindexAction.doExecute() gates every request behind two checks before any reindex
// parameter is even parsed: (1) the caller must be an authenticated admin member, and
// (2) the request must carry a csrfToken matching the one stored in the caller's HTTP
// session (set when the settings page in 01-clusterReindexSettings.cy.ts is rendered).
// ReindexActionTest.java deliberately targets the package-private handleAction() and
// skips these two gates, calling them "integration scope" - this spec is that integration
// coverage, added as part of the cluster-reindex-hardening review (see fb96b60 / 3b900c0).
//
// The action URL mirrors the construction in globalSettings.clusterlucenereindex.jsp:
// {contextPath}/cms/render/{workspace}/{lang}{currentNode.getPath()}.clusterReindex.do
// with workspace=default, lang=en and nodePath=/settings (the fixed global settings node,
// confirmed by the settings.clusterlucenereindex.html render URL used in the other spec).
const actionUrl = '/cms/render/default/en/settings.clusterReindex.do';

describe('Cluster Lucene Reindex - action security', () => {
    it('rejects an unauthenticated request with 403, before any reindex parameter is read', () => {
        cy.clearCookies();

        cy.request({
            method: 'POST',
            url: actionUrl,
            form: true,
            body: {action: 'reindexfull', csrfToken: 'irrelevant'},
            failOnStatusCode: false
        }).then(response => {
            expect(response.status).to.equal(403);
        });
    });

    it('rejects an authenticated admin request that omits the CSRF token with 403', () => {
        cy.login();

        cy.request({
            method: 'POST',
            url: actionUrl,
            form: true,
            body: {action: 'reindexfull'},
            failOnStatusCode: false
        }).then(response => {
            expect(response.status).to.equal(403);
        });
    });

    it('rejects an authenticated admin request whose CSRF token does not match the session with 403', () => {
        cy.login();
        // Rendering the settings page stores a fresh csrfToken in the HTTP session,
        // even though the non-cluster branch never prints it into the page.
        cy.visit('/cms/adminframe/default/en/settings.clusterlucenereindex.html');

        cy.request({
            method: 'POST',
            url: actionUrl,
            form: true,
            body: {action: 'reindexfull', csrfToken: 'this-does-not-match-the-session-token'},
            failOnStatusCode: false
        }).then(response => {
            expect(response.status).to.equal(403);
        });
    });
});
