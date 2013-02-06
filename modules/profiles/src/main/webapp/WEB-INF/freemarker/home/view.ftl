<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#assign isShowTutors=adminDepartments?has_content || isAPersonalTutor>
<#escape x as x?html>

<#if !user.loggedIn>
	<p>
		You're currently not signed in. <a class="sso-link" href="<@sso.loginlink />">Sign in</a>
		to see a personalised view.
	</p>
<#else>
	<#include "../profile/search/form.ftl" />
	
	<#if isShowTutors??>
		<h2>Personal tutors</h2>

		<ul>
			<#if adminDepartments?has_content>
				<#list adminDepartments as dept>
					<li><a href="<@routes.tutors dept />">Personal tutors for ${dept.name}</a></li>
				</#list>
			</#if>
	
			<#if isAPersonalTutor??>
				<li><a href="<@routes.tutees />">My personal tutees</a></li>
			</#if>
		</ul>
	</#if>
</#if>
</#escape>