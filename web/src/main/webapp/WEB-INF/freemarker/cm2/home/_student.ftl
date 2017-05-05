<#import "*/coursework_components.ftl" as components />
<#escape x as x?html>

<h1>Assignments</h1>

<#if !studentInformation.empty>
	<@components.student_assignment_list id="todo" title="Action required" assignments=studentInformation.unsubmittedAssignments show_submission_progress=true />
	<@components.student_assignment_list id="doing" title="No action required" assignments=studentInformation.inProgressAssignments />
	<@components.student_assignment_list id="done" title="Complete" assignments=studentInformation.pastAssignments />
<#else>
	You do not currently have any assignments on Tabula for ${academicYear.toString}.
</#if>

</#escape>