<%@ page language="java" contentType="text/html;charset=UTF-8" %>
<%@ page import="org.jahia.modules.lucenereindex.flow.ReindexManager" %>
<%@ page import="org.jahia.osgi.BundleUtils" %>
<%@ page import="org.jahia.services.render.RenderContext" %>
<%@ page import="org.jahia.services.content.JCRNodeWrapper" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="workspace" type="java.lang.String"--%>
<%
    RenderContext renderContext = (RenderContext) request.getAttribute("renderContext");
    JCRNodeWrapper currentNode = (JCRNodeWrapper) request.getAttribute("currentNode");
    String workspace = (String) request.getAttribute("workspace");

    ReindexManager reindexManager = BundleUtils.getOsgiService(ReindexManager.class, null);
    boolean isCluster = reindexManager != null && reindexManager.isCluster();
    java.util.HashSet clusterNodes = isCluster ? reindexManager.getClusterNodes() : null;
    pageContext.setAttribute("isCluster", isCluster);
    pageContext.setAttribute("clusterNodes", clusterNodes);

    String locale = renderContext != null && renderContext.getMainResource() != null
            ? renderContext.getMainResource().getLocale().toString() : "en";
    String nodePath = currentNode != null ? currentNode.getPath() : "/settings";
    String actionUrl = request.getContextPath()
            + "/cms/render/" + workspace
            + "/" + locale
            + nodePath
            + ".clusterReindex.do";
    pageContext.setAttribute("actionUrl", actionUrl);
%>

<div class="page-header">
    <h2>Cluster Lucene Reindexation</h2>
</div>

<div class="row">
    <div class="col-md-12">

<c:if test="${not isCluster}">
  <h3>Current environment is not a Cluster.</h3>
</c:if>
<c:if test="${isCluster}">
  <div role="alert" style="background-color: #E0182D; color: #FFFFFF; padding: 15px; margin-bottom: 20px;">
    <strong>&#9888; Warning:</strong> Be careful when starting indexation. It can take a long time and can be triggered multiple times!
  </div>
  <h3>The following cluster nodes can be reindexed:</h3>
  <form id="clusterReindexForm" action="${actionUrl}" method="post">
  <table class="table table-bordered table-striped table-sortable" aria-label="Cluster nodes available for reindexing">
     <thead>
        <tr>
            <th class="headerSortable">Cluster member ID</th>
            <th class="headerSortable">Action</th>
        </tr>
     </thead>
     <tbody>
  <c:forEach var="node" items="${clusterNodes}">
    <tr>
      <td>${node.id}</td>
      <td>
        <button type="submit" name="action" value="addreindex:${node.id}" class="btn btn-default btn-raised" aria-label="Reindex ${node.id}">Reindex</button>
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
<div id="clusterReindexMsg"></div>
<script>
(function($) {
    var lastAction = null;
    $('#clusterReindexForm button[type="submit"]').on('click', function() {
        lastAction = $(this).val();
    });
    $('#clusterReindexForm').on('submit', function(e) {
        e.preventDefault();
        var $msg = $('#clusterReindexMsg').empty();
        $.post($(this).attr('action'), {action: lastAction || ''})
            .done(function() {
                $msg.html('<div class="alert alert-success" role="status">Reindex successfully triggered.</div>');
            })
            .fail(function(xhr) {
                $msg.html('<div class="alert alert-danger" role="alert">Error (' + xhr.status + '): failed to trigger reindex.</div>');
            });
    });
})(jQuery);
</script>

    </div>
</div>
