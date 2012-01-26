<#escape x as x?html>
<#compress>

<#macro results collection title>
<h3>${title}</h3>
<#if collection?size = 0>
<p>No results.</p>
<#else>
<table>
	<tr>
		<th>Name</th>
		<th>Usercode</th>
	</tr>
<#list collection as u>
	<tr>
		<td>${u.fullName}</td>
		<td class="user-id">${u.userId}</td>
	</tr>
</#list>
</table>
</#if>
</#macro>

<@results staff "Staff" />
<@results students "Students" />

</#compress>
</#escape>