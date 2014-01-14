<#import "../related_students/related_students_macros.ftl" as student_macros />

<#escape x as x?html>
<div id="relationships">
	<h1>My ${relationshipType.studentRole}s</h1>

	<#if students?has_content>
 		<#assign submitUrl>/profiles/tutor/students</#assign>
		<#assign filterCommand = viewRelatedStudentsCommand />
		<#assign filterCommandName = "viewRelatedStudentsCommand" />
		<#assign filterResultsPath = "/WEB-INF/freemarker/relationships/student_view_results.ftl" />
		<#include "/WEB-INF/freemarker/filter_bar.ftl" />
	<#else>
		<p class="alert alert-warning"><i class="icon-warning-sign"></i> No ${relationshipType.studentRole}s are currently visible for you in Tabula.</p>
	</#if>
</div>



</#escape>
