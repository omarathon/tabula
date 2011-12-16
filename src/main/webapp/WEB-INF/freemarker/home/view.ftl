<#escape x as x?html>

<#if ownedDepartments?? && ownedDepartments?size gt 0>
<#list ownedDepartments as department>
	<div class="admin-flash">
	You're a departmental administrator for ${department.name}.
	<a href="<@url page="/admin/department/${department.code}/"/>">
		Go to the ${department.name} admin page
	</a>
	</div>
</#list>
</#if>

<#if user.loggedIn && user.firstName??>
<h1>Hello, ${user.firstName}.</h1>
<#else>
<h1>Hello.</h1>
</#if>

<p>This is the in-development coursework submission application.
	It isn't quite ready for use yet, but you can keep up with news about it by
	going to <a href="http://go.warwick.ac.uk/amupdates">go.warwick.ac.uk/amupdates</a>.</p>

<#if moduleWebgroups?? && moduleWebgroups?size gt 0>
<p>These are the modules we think you're enrolled in.</p>
<#list moduleWebgroups as pair>
<div>
<a href="<@url page='/module/${pair._1}/' />">${pair._1?upper_case}</a>
</div>
</#list>
</#if>

</#escape>