<#escape x as x?html>
	<h1>Handler mappings</h1>

	<table class="table table-striped">
		<thead>
			<th>Pattern</th>
			<th>Methods</th>
			<th>Params</th>
			<th>Headers</th>
			<th>Consumes</th>
			<th>Produces</th>
		</thead>
		<tbody>
			<#list mappings as mapping>
				<tr>
					<td>${mapping.pattern}</td>
					<td>${mapping.methods}</td>
					<td>${mapping.params}</td>
					<td>${mapping.headers}</td>
					<td>${mapping.consumes}</td>
					<td>${mapping.produces}</td>
				</tr>
			</#list>
		</tbody>
	</table>
</#escape>