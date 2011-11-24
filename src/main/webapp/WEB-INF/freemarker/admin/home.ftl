<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#escape x as x?html>

<#if ownedDepartments?size gt 0>
<#if ownedDepartments?size gt 1>
<p>You are an administrator for multiple departments. Please choose a department to manage.</p>
</#if>
<#list ownedDepartments as department>
<ul>
<li><a href="<@url page="/admin/department/${department.code}/"/>">Manage ${department.name}</a></li>
</ul>
</#list>
<#else>

Nope

</#if> 

</#escape>