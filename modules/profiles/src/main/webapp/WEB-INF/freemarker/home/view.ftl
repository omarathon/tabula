<#escape x as x?html>

<#if user.loggedIn && user.firstName??>
<h1>Hello, ${user.firstName}.</h1>
<#else>
<h1>Hello.</h1>
</#if>	

<p>
Here is where you'd see student profiles.
</p>

<#if !user.loggedIn>
<p>
You're currently not signed in. <a class="sso-link" href="<@sso.loginlink />">Sign in</a>
to see a personalised view.
</p>
</#if>

</#escape>