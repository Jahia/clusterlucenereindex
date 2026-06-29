# clusterlucenereindex

A Jahia community module that lets a server administrator trigger a Lucene
re-indexation on the nodes of a Jahia cluster, individually or all at once.

This module must be installed on a Jahia **cluster** environment (>= 8.1.0.0) and
activated for the **systemsite**.

## How it works

- A server-settings panel (**Administration → Server → System Health → Cluster Lucene
  Index**) lists the current cluster members and offers a *Reindex* button per member
  plus a *Reindex all* button.
- Clicking a button posts to the `clusterReindex` action, which appends an entry to a
  small JCR queue under `/settings/reindexAdmin` (one `jnt:nodeToReindex` node carrying
  the target cluster member id).
- On every cluster node a background watchdog (`ClusterReindexationService`, scan interval
  configurable via OSGi, default 30s) polls the queue and, when it finds an entry for *its
  own* member id, triggers the local Lucene reindex and removes the entry. This is how each
  node reindexes its own search index.

## Permissions

The `clusterReindex` action and the settings panel both require the **`adminUsers`**
server-administration permission.

## Build & test

Requires JDK 17 and Maven.

```bash
mvn clean install   # compiles, runs the unit tests (JUnit + Mockito) and packages the bundle
```

## Known limitations

- UI strings in the JSP views are currently English-only (i18n externalisation is pending).
- The reindex queue has no automatic expiry: an entry queued for a cluster member that
  leaves the cluster before claiming it is not swept automatically.

See `CHANGELOG.md` for the detailed history.
