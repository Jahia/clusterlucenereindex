import {enableModule} from '@jahia/cypress';

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

// The platform rejects these requests before ReindexAction runs, and its status is what comes back:
// guest cannot read /settings, which Jahia hides as 404, and Jahia's CSRF Guard filter stops a POST
// without a valid token with 400 (8.2.3.x). The action's own gates answer 403 where a request does
// reach it. What must hold is the rejection, so each test accepts the status of whichever layer
// rejects first, and never a 2xx.
const REJECTED_UNAUTHENTICATED = [403, 404];
const REJECTED_BAD_CSRF = [400, 403];

describe('Cluster Lucene Reindex - action security', () => {
    before(() => {
        enableModule('clusterlucenereindex', 'systemsite');
    });

    it('rejects an unauthenticated request before any reindex parameter is read', () => {
        cy.clearCookies();

        cy.request({
            method: 'POST',
            url: actionUrl,
            form: true,
            body: {action: 'reindexfull', csrfToken: 'irrelevant'},
            failOnStatusCode: false
        }).then(response => {
            expect(response.status).to.be.oneOf(REJECTED_UNAUTHENTICATED);
        });
    });

    it('rejects an authenticated admin request that omits the CSRF token', () => {
        cy.login();

        cy.request({
            method: 'POST',
            url: actionUrl,
            form: true,
            body: {action: 'reindexfull'},
            failOnStatusCode: false
        }).then(response => {
            expect(response.status).to.be.oneOf(REJECTED_BAD_CSRF);
        });
    });

    it('rejects an authenticated admin request whose CSRF token does not match the session', () => {
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
            expect(response.status).to.be.oneOf(REJECTED_BAD_CSRF);
        });
    });
});
