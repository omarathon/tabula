<#escape x as x?html>

<#if !user.loggedIn>
	<p>
		You're currently not signed in. <a class="sso-link" href="<@sso.loginlink />">Sign in</a>
		to see a personalised view.
	</p>
<#else>
	<div class="row-fluid">
		<div class="span6">
			<#include "../profile/search/form.ftl" />
	
			<#if isAPersonalTutor>
				<h2>My students</h2>
			
				<ul>
					<li><a href="<@routes.tutees />">Personal tutees</a></li>
				</ul>
			</#if>
		</div>
		
		<div id="profile-dept-admin" class="span4 offset2">
			<#if adminDepartments?has_content>
				<h4>Departmental admin</h4>
		
				<ul>
					<#list adminDepartments as dept>
						<li><a href="<@routes.tutors dept />">Personal tutors in ${dept.name}</a></li>
					</#list>
				</ul>
			</#if>
		</div>
	</div>
</#if>
</#escape>