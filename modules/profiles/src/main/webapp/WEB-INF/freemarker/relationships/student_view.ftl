<#import "../related_students/related_students_macros.ftl" as student_macros />

<#escape x as x?html>
<div id="relationships">
	<h1>My ${relationshipType.studentRole}s</h1>

 		<#assign submitUrl>/profiles/tutor/students</#assign>
		<#assign filterCommand = viewRelatedStudentsCommand />
		<#assign filterCommandName = "viewRelatedStudentsCommand" />
		<#assign filterResultsPath = "/WEB-INF/freemarker/relationships/student_view_results.ftl" />
		<#include "/WEB-INF/freemarker/filter_bar.ftl" />

</div>
</#escape>
