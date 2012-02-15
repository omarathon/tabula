<#escape x as x?html>

<#macro link_to_department department>
<a href="<@url page="/admin/department/${department.code}/"/>">
	Go to the ${department.name} admin page
</a>
</#macro>

<#if ownedDepartments?? && ownedDepartments?size gt 0>
<#list ownedDepartments as department>
	<div class="admin-flash">
	You're a departmental administrator for ${department.name}.
	<@link_to_department department />
	</div>
</#list>
</#if>

<#if ownedModuleDepartments?? && ownedModuleDepartments?size gt 0>
<#list ownedModuleDepartments as department>
	<div class="admin-flash">
	You're a manager for one or more modules in ${department.name}.
	<@link_to_department department />
	</div>
</#list>
</#if>

<#if user.loggedIn && user.firstName??>
<h1>Hello, ${user.firstName}.</h1>
<#else>
<h1>Hello.</h1>
</#if>	

<p>
This is a new service for managing coursework assignments and feedback. If you're a student,
you might start getting emails containing links to download your feedback from here.
</p>

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