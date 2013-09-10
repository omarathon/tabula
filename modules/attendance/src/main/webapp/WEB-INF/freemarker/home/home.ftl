<h1>Attendance Monitoring</h1>
<p></p>
<#if (permissionMap["View"]?size > 0)>
	<h2>View and record monitoring points</h2>
	<ul class="links">
		<#list permissionMap["View"] as department>
			<li>
				<a href="<@url page="/${department.code}"/>">${department.name}</a>
			</li>
		</#list>
	</ul>
<#else>
	<p><em>You do not have permission to view or record any monitoring points.</em></p>
</#if>

<#if (permissionMap["Manage"]?size > 0)>
	<h2>Manage monitoring points</h2>
	<ul class="links">
		<#list permissionMap["Manage"] as department>
			<li>
				<a href="<@url page="/manage/${department.code}"/>">${department.name}</a>
			</li>
		</#list>
	</ul>
</#if>