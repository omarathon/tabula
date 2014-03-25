<#escape x as x?html>

<#macro link_to_department department>
	<a href="<@routes.departmenthome department />">
		Go to the ${department.name} admin page
	</a>
</#macro>

<#if user.loggedIn && user.firstName??>
	<h1 class="with-settings">Hello, ${user.firstName}</h1>
<#else>
	<h1 class="with-settings">Hello</h1>
</#if>	

<#include "_admin.ftl" />

</#escape>
