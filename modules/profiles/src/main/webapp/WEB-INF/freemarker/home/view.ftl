<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>

<#if user.loggedIn && user.firstName??>
<h1>Hello, ${user.firstName}.</h1>
<#else>
<h1>Hello.</h1>
</#if>	

<#if features.profiles>
<div class="hero-unit">
	<h2>Search for a profile</h2>
	
	<@f.form method="post" action="${url('/search')}" commandName="searchProfilesCommand" cssClass="form-search">
		<@f.input path="query" cssClass="input-large search-query" />
		<button type="submit" class="btn">Search</btn>
	</@f.form>
</div>
<#else>
<p>Here is where you'd see student profiles.</p>
</#if>

</#escape>