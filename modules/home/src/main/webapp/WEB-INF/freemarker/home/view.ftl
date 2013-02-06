<#escape x as x?html>

<#if user.loggedIn && user.firstName??>
	<h5>Hello, ${user.firstName}</h5>
<#else>
	<h5>Hello</h5>
</#if>	

<#if !user.loggedIn>
	<p>
		You're currently not signed in. <a class="sso-link" href="<@sso.loginlink />">Sign in</a>
		to see a personalised view.
	</p>
<#else>
	<ul id="home-list">
		<#-- TODO can we guard this, or would it be too expensive to be worth the bother? -->
		<li><h2><a href="<@url page="/coursework/" />">Coursework Management</a></h2></li>
	
		<#if user.staff>
			<li><h2><a href="<@url page="/profiles/" />">Student Profiles</a></h2></li>
		<#elseif user.student>
			<li><h2><a href="<@url page="/profiles/" />">My Student Profile</a></h2></li>
		</#if>
	</ul>
</#if>

<p id="what-is-tabula">
	<i class="icon-info-sign"></i> <span title="Tabula was originated as 'My Department'.">Tabula</span> supports the administration of teaching and learning in academic departments at Warwick.
</p>

</#escape>