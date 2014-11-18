<#escape x as x?html>

	<#if user.loggedIn && user.firstName??>
		<h1 class="with-settings">Hello, ${user.firstName}</h1>
	<#else>
		<h1 class="with-settings">Hello</h1>
	</#if>

	<p class="lead muted">
		This is a service for viewing reports for various aspects of Tabula.
	</p>

</#escape>