<#compress><#escape x as x?html>
	<h1>Roles and capabilities</h1>

	<table class="table table-striped table-bordered table-condensed roles-table">
		<thead>
			<th>Permission</th>
			<#list (rolesTable?first)._2() as roles>
				<th>${roles._1().description}</th>
			</#list>
		</thead>
		<tbody>
			<#list rolesTable as permission>
				<tr>
					<td>${permission._1().description}</td>
					<#list permission._2() as roles>
						<td>
							<#if roles._2()?has_content>
								<#if roles._2()>
									<i class="icon-ok attended"></i>
								<#else>
									<i class="icon-remove unauthorised"></i>
								</#if>
							<#else>
								<i class="icon-remove-circle authorised"></i>
							</#if>
						</td>
					</#list>
				</tr>
			</#list>
		</tbody>
	</table>
</#escape></#compress>