<#escape x as x?html>

<#macro link_to_department department>
<a href="<@url page="/admin/department/${department.code}/"/>">
	Go to the ${department.name} admin page
</a>
</#macro>

<#if user.loggedIn && user.firstName??>
<h1>Hello, ${user.firstName}.</h1>
<#else>
<h1>Hello.</h1>
</#if>	

<p>
This is a new service for managing coursework assignments and feedback. If you're a student,
you might start getting emails containing links to download your feedback from here.
</p>

<#if !user.loggedIn>
<p>
You're currently not signed in. <a class="sso-link" href="<@sso.loginlink />">Sign in</a>
to see a personalised view.
</p>
</#if>

<#if nonempty(ownedDepartments) || nonempty(ownedModuleDepartments)>
<h2>Administration</h2>
</#if>

<#if nonempty(ownedDepartments)>
<p>You're a departmental administrator.</p>
<ul class="links">
<#list ownedDepartments as department>
	<li>
	<@link_to_department department />
	</li>
</#list>
</ul>
</#if>

<#if nonempty(ownedModuleDepartments)>
<p>You're a manager for one or more modules.</p>
<ul class="links">
<#list ownedModuleDepartments as department>
	<li>
	<@link_to_department department />
	</li>
</#list>
</ul>
</#if>

<#assign has_feedback=(assignmentsWithFeedback?? && assignmentsWithFeedback?size gt 0) />
<#assign has_submissions=(assignmentsWithSubmission?? && assignmentsWithSubmission?size gt 0) />

<#if has_feedback || has_submissions>
<h2>Your assignments</h2>

<ul class="links">

<#if has_feedback>
<#list assignmentsWithFeedback as assignment>
	<li class="assignment-info">
		<span class="label-green">Marked</span>
		<a href="<@url page='/module/${assignment.module.code}/${assignment.id}/' />">
			${assignment.module.code?upper_case} (${assignment.module.name}) - ${assignment.name}
		</a>
	</li>
</#list>
</#if>

<#if has_submissions>
<#list assignmentsWithSubmission as assignment>
	<li class="assignment-info">
		<span class="label-orange">Submitted</span>
		<a href="<@url page='/module/${assignment.module.code}/${assignment.id}/' />">
			${assignment.module.code?upper_case} (${assignment.module.name}) - ${assignment.name}
		</a>
	</li>
</#list>
</#if>

</ul>
</#if>

<#--
<#if moduleWebgroups?? && moduleWebgroups?size gt 0>
<p>These are the modules we think you're enrolled in.</p>
<#list moduleWebgroups as pair>
<div>
<a href="<@url page='/module/${pair._1}/' />">${pair._1?upper_case}</a>
</div>
</#list>
</#if>
-->

</#escape>