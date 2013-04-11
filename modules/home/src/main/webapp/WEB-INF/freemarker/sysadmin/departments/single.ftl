<#escape x as x?html>

<h1>${department.name}</h1>

<p><a href="permissions/">View department admins</a></p>

<#if department.parent??>
<p>Parent department: <a href='<@url page="/sysadmin/departments/${department.parent.code}/" />'>${department.parent.name}</a></p>
</#if>

<#if department.children?size gt 0>
<p>Sub-departments:</p>
<ul>
<#list department.children as child>
	<li><a href='<@url page="/sysadmin/departments/${child.code}/" />'>${child.name}</a></li>
</#list>
</ul>
</#if>

${department.modules?size} modules

<ul>
<#list department.modules as module>
<li>${module.code} - ${module.name!"Unknown"}</li>
</#list>
</ul>

</#escape>