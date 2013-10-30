<#escape x as x?html>

<h1>Manage monitoring points</h1>

<#if departments?size == 0>
	<p><em>You do not have permission to manage the monitoring points for any department.</em></p>
<#else>
	<p><em>Choose the department to manage:</em></p>
	<ul class="links">
		<#list departments as department>
			<li>
				<a href="<@routes.manageDepartment department />">${department.name}</a>
			</li>
		</#list>
	</ul>
</#if>

</#escape>