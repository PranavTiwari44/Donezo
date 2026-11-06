<%@ include file="common/header.jspf" %>
<%@ include file="common/navigation.jspf" %>
	<div class="container">
	   <h1>Welcome <c:out value="${name}"/></h1>
	   <h2><a href = "list-todos">Manage</a> your Todos!</h2>
	</div>
<%@ include file="common/footer.jspf" %>