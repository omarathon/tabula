<#escape x as x?html>

<#if user.loggedIn && user.firstName??>
<h1>Hello, ${user.firstName}</h1>
<#else>
<h1>Hello</h1>
</#if>	

<#if !user.loggedIn>
	<p>
		You're currently not signed in. <a class="sso-link" href="<@sso.loginlink />">Sign in</a>
		to see a personalised view.
	</p>
<#else>
	<div class="well">
		<h5>Where now?</h5>
		
		<ul class="lead">
			<#-- TODO can we guard this, or would it be too expensive to be worth the bother? -->
			<li><a href="<@url page="/coursework/" />">Coursework Management</a></li>
		
			<#if user.staff>
				<li><a href="<@url page="/profiles/" />">Student Profiles</a></li>
			<#elseif user.student>
				<li><a href="<@url page="/profiles/" />">My Student Profile</a></li>
			</#if>
		</ul>
	</div>
</#if>

<p class="muted">
	<i class="icon-info-sign"></i> <span title="Tabula was originated as 'My Department'.">Tabula</span> supports the administration of teaching and learning in academic departments at Warwick.
</p>

</#escape>