<#compress>
<#assign warwick=JspTaglibs["/WEB-INF/tld/warwick.tld"]>

<#macro module_name module>
	<span class="mod-code">${module.code?upper_case}</span> <span class="mod-name">${module.name}</span>
</#macro>

<#macro assignment_name assignment>
	<@module_name assignment.module /> <span class="ass-name">${assignment.name}</span>
</#macro>

<#macro assignment_link assignment>
	<@module_name assignment.module />
	<a href="<@url page='/module/${assignment.module.code}/${assignment.id}/' />">
		<span class="ass-name">${assignment.name}</span>
	</a>
</#macro>

<#macro date date at=false timezone=false seconds=false capitalise=true relative=true><#--
-->${dateBuilder(date, seconds, at, timezone, capitalise, relative)}<#--
--></#macro>

<#macro p number singular plural="${singular}s" one="1" zero="0" shownumber=true><#--
--><#if shownumber><#if number=1>${one}<#elseif number=0>${zero}<#else>${number}</#if><#--
--> </#if><#if number=1>${singular}<#else>${plural}</#if></#macro>

<#macro tense date future past><#if date.afterNow>${future}<#else>${past}</#if></#macro>

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
