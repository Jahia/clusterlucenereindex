# Changelog

All notable changes to the clusterlucenereindex module are documented in this file.

## [Unreleased]

### Security
- **The `clusterReindex` action now requires the `adminUsers` permission.** Triggering a
  (cluster-wide) Lucene reindex is a heavy, privileged operation; previously the action
  declared no required permission, so the Jahia dispatcher let any authenticated user
  invoke it (a denial-of-service vector). The UI-side checks only hid the button. The
  action input (`nodeId`) is also trimmed and rejected when blank.

### Fixed
- **Reindex requests are no longer silently lost.** The watchdog now schedules the local
  reindex *before* removing the queue entry, so a failure while scheduling no longer drops
  the request. The Jackrabbit repository cast is guarded with `instanceof` and logs a clear
  error instead of throwing `ClassCastException` if the internal type ever changes.
- **The watchdog can no longer die silently.** `java.util.Timer` was replaced with a
  `ScheduledExecutorService` (single named daemon thread, fixed delay); every periodic run
  is wrapped so that no exception can cancel all future runs.
- **Null-safety around cluster discovery.** When Karaf Cellar is unavailable, or a cluster
  member id cannot be resolved, the module now logs an actionable warning and skips that
  member instead of writing a `null` id or letting a `NullPointerException` bubble through a
  broad catch.
- Reindex queue node names use a UUID suffix instead of `System.currentTimeMillis()`,
  removing a name-collision risk when two requests are queued within the same millisecond.

### Accessibility / UX
- The reindex buttons now ask for confirmation before triggering the operation, show an
  in-progress message and disable themselves while the request is in flight.
- The cautionary banner uses `role="note"` (it is static, not a live alert) with a darker
  red that meets the enhanced (AAA) contrast threshold against white text.
- Cluster member ids are HTML-escaped (`fn:escapeXml`) wherever they are rendered.
- Keyboard form submission is honoured via the event `submitter`; the live-status region no
  longer carries conflicting child roles.
- The "not a cluster" message is a paragraph rather than a heading.

### Changed / Removed
- Extracted the queue dedup logic into a package-private `addNodeForReindexIfAbsent` method
  and narrowed reflection error handling to `ReflectiveOperationException`.
- Removed dead template scaffolding: the broken `virtualsite` "Settings example" view (it
  emitted a nested full HTML document) and the unused `rules.drl.disabled` /
  `clusterlucenereindex.tld.disabled` placeholders.

### Tests
- Added JaCoCo coverage and the first `ReindexManager` unit tests (dedup logic, mocked JCR
  session) and a blank-`nodeId` case for `ReindexAction`.

### Known limitations
- UI strings in the JSP views are still hardcoded English; full externalisation into the
  `clusterlucenereindex*.properties` bundles is deferred (it could not be render-validated
  in the current build-only environment).
- The reindex queue has no time-to-live: an entry enqueued for a cluster member that leaves
  the cluster before claiming it is not swept automatically.
