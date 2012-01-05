<#compress>
<#assign warwick=JspTaglibs["/WEB-INF/tld/warwick.tld"]>
<#macro module_name module>
<span class="code">${module.code?upper_case}</span> <span class="name">(${module.name})</span>
</#macro>
<#macro date date>
<@warwick.formatDate value=date pattern="d MMMM yyyy HH:mm:ss" />
</#macro>
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
</#compress>
