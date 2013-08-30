<#import "../related_students/related_students_macros.ftl" as student_macros />

<#escape x as x?html>
<div id="relationships">
	<h1>My ${relationshipType.studentRole}s</h1>
	
	<#if students?has_content>

		<@student_macros.table students=students is_relationship=true />

	<#else>
		<p class="alert alert-warning"><i class="icon-warning-sign"></i> No ${relationshipType.studentRole}s are currently visible for you in Tabula.</p>
	</#if>
</div>

</#escape>