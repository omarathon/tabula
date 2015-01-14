<#compress><#escape x as x?html>
<#import "*/permissions_macros.ftl" as pm />
<#import "*/_profile_link.ftl" as pl />

<#function roleDescription definition>
	<#if definition.baseRoleDefinition??>
		<#if definition.replacesBaseDefinition>
			<#return roleDescription(definition.baseRoleDefinition) />
		<#else>
			<#local result><span title="Derived from ${definition.baseRoleDefinition.description}">${definition.name}</span></#local>
			<#return result />
		</#if>
	<#else>
		<#return definition.description />
	</#if>
</#function>

<#macro render_tree node>
	${node.target.humanReadableId} (${node.target.urlCategory}) <a class="btn btn-mini" href="<@routes.permissions node.target />">Edit permissions</a>
	<ul>
		<#list node.roles as role>
			<li>
				<i class="icon-user" title="Role"></i> <#noescape>${roleDescription(role.definition)}</#noescape>: <@render_users role.users />
			</li>
		</#list>
		<#list node.permissions as permission>
			<li>
				<i class="icon-lock" title="Permission"></i> ${permission.permission.description}: <@render_users permission.users />
			</li>
		</#list>
		<#list node.children as child>
			<li><@render_tree child /></li>
		</#list>
	</ul>
</#macro>

<#macro render_users users>
	<#if users?size == 0>
		None
	<#elseif users?size == 1>
		${(users?first).fullName} (${(users?first).userId}) <@pl.profile_link (users?first).warwickId! />
	<#else>
		<#local popoverContent>
			<ul>
				<#list users as user>
					<li>${user.fullName} (${user.userId}) <@pl.profile_link user.warwickId! /></li>
				</#list>
			</ul>
		</#local>

		<a class="use-popover" data-content="<#noescape>${popoverContent?html}</#noescape>" data-html="true" data-placement="right">
			${users?size} people
		</a>
	</#if>
</#macro>

<#if target.urlCategory == 'department'>
	<@fmt.deptheader "Roles and permissions" "for" target routes "permissionstree" "" />
<#else>
	<h1>Roles and permissions</h1>
	<h5>
		<span class="muted">for</span>
		${target.humanReadableId}
	</h5>
</#if>

<ul>
	<li><@render_tree tree /></li>
</ul>

<div id="profile-modal" class="modal fade profile-subset"></div>
<script type="text/javascript">
	jQuery(function($) {
		$('.use-popover').on('shown', function() {
			$('a.ajax-modal').ajaxModalLink();
		});
	});
</script>

</#escape></#compress>