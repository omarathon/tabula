<#compress>
<#assign warwick=JspTaglibs["/WEB-INF/tld/warwick.tld"]>

<#macro module_name module>
<span class="mod-code">${module.code?upper_case}</span> <span class="mod-name">(${module.name})</span>
</#macro>

<#macro date date>
<@warwick.formatDate value=date pattern="d MMMM yyyy HH:mm:ss" />
</#macro>

<#macro p number singular plural="${singular}s">${number} <#if number=1>${singular}<#else>${plural}</#if></#macro>

<#macro usergroup_summary ug>
<div class="usergroup-summary">
<#if ug.baseWebgroup??>
	Webgroup "${ug.baseWebgroup}" (${ug.baseWebgroupSize} members)
	<#if ug.includeUsers?size gt 0>
	+${ug.includeUsers?size} extra users
	</#if>
	<#if ug.excludeUsers?size gt 0>
	-${ug.excludeUsers?size} excluded users
	</#if>
<#else>
	<#if ug.includeUsers?size gt 0>
	${ug.includeUsers?size} users
	</#if>
</#if>
</div>
</#macro>

<#-- comma separated list of users by name -->
<#macro user_list_csv ids>
<@userlookup ids=ids>
	<#list returned_users?keys?sort as id>
		<#assign returned_user=returned_users[id] />
		<#if returned_user.foundUser>
			${returned_user.fullName}<#if id_has_next>,</#if>
		<#else>
			${id}<#if id_has_next>,</#if>
		</#if>
	</#list>
	</@userlookup>
</#macro>
</#compress>
