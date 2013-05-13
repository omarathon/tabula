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
				<h4>Departmental administration</h4>
		
				<#list adminDepartments as dept>
					<h5>${dept.name}</h5>
					
					<div class="btn-group module-manage-button">
					  <a class="btn btn-medium dropdown-toggle" data-toggle="dropdown"><i class="icon-wrench"></i> Manage <span class="caret"></span></a>
					  <ul class="dropdown-menu pull-right">	
							<li><a href="<@routes.deptperms dept/>">
								<i class="icon-user"></i> Edit departmental permissions
							</a></li>
							
							<li><a href="<@routes.tutors dept />">Personal tutors</a></li>
							<li><a href="<@routes.tutors_missing dept />">Students with no personal tutor</a></li>
					  </ul>
					</div>
				</#list>
			</#if>
		</div>
	</div>
</#if>
</#escape>