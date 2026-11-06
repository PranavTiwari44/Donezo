<%@ include file="common/header.jspf" %>
<%@ include file="common/navigation.jspf" %>
	<div class="container">
		   <H1>Your Todos</H1>
		   <table class="table">
				<thead>
					<tr>
						<th>Description</th>
						<th>Target Date</th>
						<th>Is Done?</th>
						<th>Edit</th>
					</tr>
				</thead>
				<tbody>
					<c:forEach items="${todos}" var="todo">
						<tr>
							<td><c:out value="${todo.description}"/></td>
							<td>${todo.targetDate}</td>
							<td>${todo.done}</td>
							<td>
								<form action="toggle-todo" method="post">
									<input type="hidden" name="id" value="${todo.id}"/>
									<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
									<button type="submit" class="btn btn-primary">${todo.done ? 'Undo' : 'Done'}</button>
								</form>
							</td>
							<td>
								<form action="delete-todo" method="post">
									<input type="hidden" name="id" value="${todo.id}"/>
									<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
									<button type="submit" class="btn btn-danger">Delete</button>
								</form>
							</td>
							<td><a href="update-todo?id=${todo.id}" class="btn btn-warning">Update</a></td>
						</tr>
					</c:forEach>
				</tbody>
		   </table>
		   <a href="add-todos" class="btn btn-success">Add Todo</a>
	   </div>
<%@ include file="common/footer.jspf" %>