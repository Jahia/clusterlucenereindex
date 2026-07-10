# Tests

End-to-end tests for the `clusterLuceneReindex` module, using the `@jahia/cypress` harness.

Two options are available to run the tests, you can either run everything in Docker or only run Jahia in Docker and run the tests using your local node.

### Run all in Docker

Once you have a built test container, the entirety of the tests, from environment provisioning to report generation, can be executed using a single command.

```bash
# Build the test container
> bash ci.build.sh
# Execute the tests
> bash ci.startup.sh
```

This is the exact process used by CI. If you use this method locally, remember that you need to **rebuild the test container** (`bash ci.build.sh`) every time you change something under `tests/`, otherwise your change never makes it into the container.

### Run the tests on a local node

This is the method you will use when developing or debugging a single test.

```bash
# Fetch the necessary javascript dependencies
> yarn
# Run the docker environment, but without the tests
> ./ci.startup.sh notests
# Provision the environment and run the tests headless once
> ./env.run.sh
# Load the environment variables (needed in every new terminal)
> source set-env.sh
> yarn run e2e:debug
```

`baseUrl` is `http://localhost:8080`; the Jahia debug port `8000` (JPDA) is exposed.

## What's covered

- `01-clusterReindexSettings.cy.ts` - the settings page renders for an admin, and correctly
  reports a non-clustered environment on this single-node harness.
- `02-clusterReindexAction-security.cy.ts` - the authorization and CSRF gates added in the
  cluster-reindex-hardening review are enforced end-to-end: an unauthenticated caller and a
  caller with a missing/mismatched CSRF token are both rejected with `403`, before any reindex
  action is dispatched.

A genuine "happy path" test (submitting a valid `csrfToken` and observing a queued reindex) needs
a real multi-node cluster, since the reindex form - and the CSRF token field - only render when
`ReindexManager.isCluster()` is true. That scenario is out of scope for this single-node harness
and is intentionally not simulated here.
