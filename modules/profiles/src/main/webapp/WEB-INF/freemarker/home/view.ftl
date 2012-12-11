<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>

<#if user.loggedIn && user.firstName??>
<h1>Hello, ${user.firstName}.</h1>
<#else>
<h1>Hello.</h1>
</#if>	

<#if !user.loggedIn>
<p>
You're currently not signed in. <a class="sso-link" href="<@sso.loginlink />">Sign in</a>
to see a personalised view.
</p>
<#else>

<div class="hero-unit">
	<h2>Search for a profile</h2>
	
	<@f.form method="post" action="${url('/search')}" commandName="searchProfilesCommand" cssClass="form-search">
		<@f.input path="query" cssClass="input-large search-query" />
		<button type="submit" class="btn">Search</btn>
	</@f.form>
</div>

</#if>

</#escape>