<h1>Fix monitoring checkpoints for attended meetings</h1>

<#if checkpoints?size == 0>

<p>There are no checkpoints to be marked as attended.</p>

<#else>

<p>These checkpoints will be marked as attended:</p>

<table class="table">
	<thead>
	<tr>
		<th>Scheme</th>
		<th>Point</th>
		<th>Student</th>
	</tr>
	</thead>
	<tbody>
	<#list checkpoints as checkpoint>
		<tr>
			<td>${checkpoint.point.scheme.name}</td>
			<td>${checkpoint.point.name}</td>
			<td>${checkpoint.student.universityId}</td>
		</tr>
	</#list>
	</tbody>
</table>

<form method="post">
	<p>
		<button class="btn btn-primary">Mark checkpoints attended</button>
	</p>
</form>

</#if>
