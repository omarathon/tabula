<#escape x as x?html>

<#macro link_to_department department>
	<a href="<@url page="/admin/department/${department.code}/"/>">
		Go to the ${department.name} admin page
	</a>
</#macro>

<#if user.loggedIn && user.firstName??>
	<h1>Hello, ${user.firstName}</h1>
<#else>
	<h1>Hello</h1>
</#if>	

<p class="lead muted">
	This is your service for managing coursework assignments and feedback
</p>

<#if !user.loggedIn>
	<p class="alert">
		You're currently not signed in. <a class="sso-link" href="<@sso.loginlink />">Sign in</a>
		to see a personalised view.
	</p>
</#if>

<#include "_student.ftl" />
<#include "_markers.ftl" />
<#include "_admin.ftl" />

</#escape>