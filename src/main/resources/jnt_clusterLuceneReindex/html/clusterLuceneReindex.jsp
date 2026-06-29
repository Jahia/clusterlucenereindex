<%@ page language="java" contentType="text/html;charset=UTF-8" %>
<%@ page import="org.jahia.modules.lucenereindex.flow.ReindexManager" %>
<%@ page import="org.jahia.osgi.BundleUtils" %>
<%@ page import="org.jahia.services.render.RenderContext" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="workspace" type="java.lang.String"--%>
<%
    RenderContext renderContext = (RenderContext) request.getAttribute("renderContext");
    String workspace = (String) request.getAttribute("workspace");

    ReindexManager reindexManager = BundleUtils.getOsgiService(ReindexManager.class, null);
    boolean isCluster = reindexManager != null && reindexManager.isCluster();
    java.util.Set<Object> clusterNodes = isCluster ? reindexManager.getClusterNodes() : null;
    pageContext.setAttribute("isCluster", isCluster);
    pageContext.setAttribute("clusterNodes", clusterNodes);

    String mainNodePath = renderContext != null && renderContext.getMainResource() != null
            ? renderContext.getMainResource().getNode().getPath() : "/settings";
    String locale = renderContext != null && renderContext.getMainResource() != null
            ? renderContext.getMainResource().getLocale().toString() : "en";
    String actionUrl = request.getContextPath()
            + "/cms/render/" + workspace
            + "/" + locale
            + mainNodePath
            + ".clusterReindex.do";
    pageContext.setAttribute("actionUrl", actionUrl);

    String csrfToken = (String) session.getAttribute("clusterReindex.csrf");
    if (csrfToken == null) {
        csrfToken = java.util.UUID.randomUUID().toString();
        session.setAttribute("clusterReindex.csrf", csrfToken);
    }
    pageContext.setAttribute("csrfToken", csrfToken);
%>

<div class="page-header">
    <h2>Cluster Lucene Reindexation</h2>
</div>

<div class="row">
    <div class="col-md-12">

<c:if test="${not isCluster}">
  <p>Current environment is not a Cluster.</p>
</c:if>
<c:if test="${isCluster}">
  <div role="note" style="background-color: #8b0000; color: #ffffff; padding: 15px; margin-bottom: 20px;">
    <strong><span aria-hidden="true">&#9888;</span> Warning:</strong> Be careful when starting indexation. It can take a long time and can be triggered multiple times!
  </div>
  <h3>The following cluster nodes can be reindexed:</h3>
  <form id="clusterReindexForm" action="${actionUrl}" method="post">
  <input type="hidden" id="csrfTokenField" value="${fn:escapeXml(csrfToken)}"/>
  <table class="table table-bordered table-striped table-sortable" aria-label="Cluster nodes available for reindexing">
     <thead>
        <tr>
            <th class="headerSortable" scope="col">Cluster member ID</th>
            <th class="headerSortable" scope="col">Action</th>
        </tr>
     </thead>
     <tbody>
  <c:forEach var="node" items="${clusterNodes}">
    <tr>
      <td><c:out value="${node.id}" escapeXml="true"/></td>
      <td>
        <button type="submit" name="action" value="addreindex:${fn:escapeXml(node.id)}" class="btn btn-default btn-raised" aria-label="Reindex cluster node ${fn:escapeXml(node.id)}">Reindex</button>
      </td>
    </tr>
  </c:forEach>
    <tr>
      <td colspan="2" style="text-align: right;">
        <button type="submit" name="action" value="reindexfull" class="btn btn-default btn-raised">Reindex all available cluster nodes</button>
      </td>
    </tr>
     </tbody>
  </table>
  </form>
</c:if>
<div id="clusterReindexMsg" aria-live="polite" aria-atomic="true"></div>
<script>
(function($) {
    var $form = $('#clusterReindexForm');
    var lastAction = null;
    $form.find('button[type="submit"]').on('click', function() {
        lastAction = $(this).val();
    });
    $form.on('submit', function(e) {
        e.preventDefault();
        // Fall back to the submitter button when the form is submitted via the keyboard.
        var action = lastAction
            || (e.originalEvent && e.originalEvent.submitter ? e.originalEvent.submitter.value : '');
        if (!action) {
            return;
        }
        // A reindex is heavy and cluster-wide: confirm before triggering it.
        if (!window.confirm('Start the Lucene reindexation now? This can take a long time on a large repository.')) {
            lastAction = null;
            return;
        }
        var $buttons = $form.find('button[type="submit"]').prop('disabled', true);
        var $msg = $('#clusterReindexMsg')
            .html('<div class="alert alert-info">Reindexation requested, please wait...</div>');
        $.post($form.attr('action'), {action: action, csrfToken: $('#csrfTokenField').val()})
            .done(function() {
                $msg.html('<div class="alert alert-success">Reindex successfully triggered.</div>');
            })
            .fail(function(xhr) {
                $msg.html('<div class="alert alert-danger">Error (' + xhr.status + '): failed to trigger reindex.</div>');
            })
            .always(function() {
                $buttons.prop('disabled', false);
                lastAction = null;
            });
    });
})(jQuery);
</script>

    </div>
</div>
