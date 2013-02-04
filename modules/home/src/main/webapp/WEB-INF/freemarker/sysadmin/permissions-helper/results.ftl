<h1>Permissions helper - results</h1>

<#macro debugPermission permission scope={}>
	<span class="permission"><i class="icon-lock"></i> ${permission.name}</span>
	<#if scope?? && scope?size != 0>
		on <span class="scope"><i class="icon-bookmark"></i> ${scope.toString}</span>
	</#if>
</#macro>

<#macro debugRole role>
	<span class="role"><i class="icon-user"></i> ${role.name}</span><#if role.scope??> on <span class="scope"><i class="icon-bookmark"></i> ${role.scope.toString}</span></#if>
	<#if role.explicitPermissions?size gt 0 || role.subRoles?size gt 0>
		<ul>
			<#list role.subRoles as subRole>
				<li><@debugRole role=subRole /></li>
			</#list>
			<#list role.explicitPermissionsAsList as permission>
				<li><@debugPermission permission=permission._1 scope=permission._2 /></li>
			</#list>
		</ul>
	</#if>
</#macro>

<table class="table">
	<tbody>
		<tr>
			<th>User</th>
			<td>${permissionsHelperCommand.user.fullName} (${permissionsHelperCommand.user.userId})</td>
		</tr>
		<#if permissionsHelperCommand.scope?? && permissionsHelperCommand.scopeType??>
			<tr>
				<th>Scope</th>
				<td>${permissionsHelperCommand.scopeType} - <span class="scope"><i class="icon-bookmark"></i> ${permissionsHelperCommand.scope}</span></td>
			</tr>
		</#if>
		<#if permissionsHelperCommand.permission??>
			<tr>
				<th>Permission</th>
				<td><span class="permission"><i class="icon-lock"></i> ${permissionsHelperCommand.permission.name}</span></td>
			</tr>
		</#if>
	</tbody>
	<tbody>
		<tr>
			<th>Result</th>
			<td>
				<i class="icon-<#if results.canDo>ok<#else>remove</#if>"></i>
				${permissionsHelperCommand.user.fullName}
				<strong><#if results.canDo>CAN<#else>CANNOT</#if></strong>
				perform <span class="permission"><i class="icon-lock"></i> ${permissionsHelperCommand.permission.name}</span>
				<#if permissionsHelperCommand.scope?? && permissionsHelperCommand.scopeType??>
					on <span class="scope"><i class="icon-bookmark"></i> ${permissionsHelperCommand.scope}</span> (${permissionsHelperCommand.scopeType})
				</#if>
			</td>
		</tr>
		<tr>
			<th>Roles<br><small>(relevant to scope)</small></th>
			<td>
				<#if results.roles?size gt 0>
					<ul>
						<#list results.roles as role>
							<li><@debugRole role=role /></li>
						</#list>
					</ul>
				<#else>
					None
				</#if>
			</td>
		</tr>
		<tr>
			<th>Explicit granted permissions<br><small>(relevant to scope)</small></th>
			<td>
				<#if results.permissions?size gt 0>
				<#else>
					None
				</#if>
			</td>
		</tr>
	</tbody>
</table>

<h2>Search again</h2>

<#include "_form.ftl" />