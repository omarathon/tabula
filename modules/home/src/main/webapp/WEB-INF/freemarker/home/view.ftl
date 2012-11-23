<#escape x as x?html>

<#if user.loggedIn && user.firstName??>
<h1>Hello, ${user.firstName}.</h1>
<#else>
<h1>Hello.</h1>
</#if>	

<p>
Tabula is a tool to support the administration of teaching and learning in academic departments. 
It's also referred to as My Department.
</p>

<#if !user.loggedIn>
<p>
You're currently not signed in. <a class="sso-link" href="<@sso.loginlink />">Sign in</a>
to see a personalised view.
</p>
<#else>
<h1><a href="<@url page="/coursework/"/>">Coursework Management</a></h1>
<h1><a href="<@url page="/profiles/"/>">Student Profiles</a></h1>
</#if>

</#escape>