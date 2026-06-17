<%@ page language="java" contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="ui" uri="http://www.jahia.org/tags/uiComponentsLib" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<%@ taglib prefix="query" uri="http://www.jahia.org/tags/queryLib" %>
<%@ taglib prefix="utility" uri="http://www.jahia.org/tags/utilityLib" %>
<%@ taglib prefix="s" uri="http://www.jahia.org/tags/search" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="out" type="java.io.PrintWriter"--%>
<%--@elvariable id="script" type="org.jahia.services.render.scripting.Script"--%>
<%--@elvariable id="scriptInfo" type="java.lang.String"--%>
<%--@elvariable id="workspace" type="java.lang.String"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="currentResource" type="org.jahia.services.render.Resource"--%>
<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>
<%--<c:url var="actionUrl" value="${flowExecutionUrl}">--%>



<div class="page-header">
    <h2>Cluster Lucene Reindexation</h2>
</div>


<div class="row">
    <div class="col-md-12">

<c:if test="${not flowModel.cluster}">
  <h3>Current environment is not a Cluster.</h3>
</c:if>
<c:if test="${flowModel.cluster}">
  <div role="alert" style="background-color: #E0182D; color: #FFFFFF; padding: 15px; margin-bottom: 20px;">
    <strong>&#9888; Warning:</strong> Be careful when starting indexation. It can take a long time and can be triggered multiple times!
  </div>
  <h3>The following cluster nodes can be reindexed:</h3>
  <form action="${flowExecutionUrl}" method="post">
  <table class="table table-bordered table-striped table-sortable" aria-label="Cluster nodes available for reindexing">
     <thead>
        <tr>
            <th class="headerSortable">Cluster member ID</th>
            <th class="headerSortable">Action</th>
        </tr>
     </thead>
     <tbody>
  <c:forEach var="node" items="${flowModel.clusterNodes}" varStatus="nodeStatus">
    <tr>
      <td>${node.id}</td>
      <td>
        <button type="submit" name="_eventId_addreindex" value="${node.id}" class="btn btn-default btn-raised" aria-label="Reindex ${node.id}">Reindex</button>
      </td>
    </tr>
  </c:forEach>
    <tr>
      <td colspan="2" style="text-align: right;">
        <button type="submit" name="_eventId_reindexfull" class="btn btn-default btn-raised">Reindex all available cluster nodes</button>
      </td>
    </tr>
     </tbody>
    </table>
  </form>
  
  
  
</c:if>      
    </div>
</div>



