<#escape x as x?html>

<#macro link_to_department department>
<a href="<@url page="/admin/department/${department.code}/"/>">
	Go to the ${department.name} admin page
</a>
</#macro>

<#if user.loggedIn && user.firstName??>
<h1>Hello, ${user.firstName}.</h1>
<#else>
<h1>Hello.</h1>
</#if>	

<p>
This is a new service for managing coursework assignments and feedback. If you're a student,
you might start getting emails containing links to download your feedback from here.
</p>

<#if !user.loggedIn>
<p>
You're currently not signed in. <a class="sso-link" href="<@sso.loginlink />">Sign in</a>
to see a personalised view.
</p>
</#if>

<#include "_admin.ftl" />

<#include "_student.ftl" />

</#escape>