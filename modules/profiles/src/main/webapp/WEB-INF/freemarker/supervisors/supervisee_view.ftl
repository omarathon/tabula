<#import "../related_students/related_students_macros.ftl" as supervisee_macros />

<#escape x as x?html>
<div id="supervisors">
	<h1>My supervisees</h1>

	<#if supervisees?has_content>

		<@supervisee_macros.table tutees=supervisees is_relationship=true />

	<#else>
		<p class="alert alert-warning"><i class="icon-warning-sign"></i> No supervisees are currently visible for you in Tabula.</p>
	</#if>
</div>

</#escape>