<#if user.loggedIn && user.firstName??>
<h1 class="with-settings">Hello, ${user.firstName}</h1>
<#else>
<h1 class="with-settings">Hello</h1>
</#if>

<p class="lead muted">
	This is a service for managing exams and feedback
</p>

<#if !user.loggedIn>
	<#if IS_SSO_PROTECTED!true>
	<p class="alert">
		You're currently not signed in. <a class="sso-link" href="<@sso.loginlink />">Sign in</a>
		to see a personalised view.
	</p>
	</#if>
</#if>