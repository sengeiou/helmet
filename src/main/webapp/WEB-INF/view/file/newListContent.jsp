<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<div class="main-content" style=" width: 95%; margin: 0 auto;">
    <%--<h3>--%>
        <%--<a class="text-muted"  href="javascript:window.history.go(-1);"><span class="lnr lnr-chevron-left"></span> 返回</a>--%>
    <%--</h3>--%>
    <h3 class="page-title">
        <c:choose>
            <c:when test="${type=='video'}">视频</c:when>
            <c:when test="${type=='image'}">图片</c:when>
            <c:when test="${type=='audio'}">声音</c:when>
            <c:when test="${type=='file'}">文件</c:when>
        </c:choose>
    </h3>
    <ul class="list-unstyled row cover_zone">
        <c:forEach var="v" items="${vo.list}">
            <li class="col-md-3 col-sm-4 col-xs-6">
                <div class="panel cover_list btnDiv-${type}" data-id="${v.id}">
                    <h4>${v.id} ${v.account}</h4>
                    <div class="helmet-${type}" data-id="${v.id}" data-mc="${v.machineCode}">
                        <c:choose>
                            <c:when test="${type=='video'}"><img src="${fileServer}${v.thumbOssPath}" alt="视频" style="cursor:pointer;" onclick="viewMedia('${type}',${v.id})" ></c:when>
                            <c:when test="${type=='image'}"><img src="${fileServer}${v.thumbOssPath}" alt="图片" style="cursor:pointer;" onclick="viewMedia('${type}',${v.id})" ></c:when>
                            <c:when test="${type=='audio'}"><a style="cursor:pointer;" onclick="viewMedia('${type}',${v.id})" >点击播放声音</a></c:when>
                            <c:when test="${type=='file'}"><a style="cursor:pointer;" onclick="viewMedia('${type}',${v.id})" >点击查看文件</a></c:when>
                        </c:choose>
                    </div>
                    <h5>${v.getCreateTimeString()}</h5>
                </div>
            </li>
        </c:forEach>
    </ul>
<jsp:include page="../include/new-page-pager.jsp"></jsp:include>
</div>
<script>
    var viewMedia = function (resType,resId) {
        var url = "/"+resType+"/play/"+resId;
        loadMainContent(url);
    }
    <c:if test="${createLoadDataFunc}">
        var loadData = function (page) {
            var url = "/list/video/searchlist/${searchKey}/${searchVal}/"+page;
            loadMainContent(url);
        }
        var reloadListData = function () {
            loadData(1);
        }
    </c:if>
    var listContentPage = {};
    listContentPage.type="${type}";
</script>
<c:if test="${isAdmin && (type=='video' || type=='image')}">
    <script src="/static/js/adminOperateRes.js?v=${version}"></script>
</c:if>

