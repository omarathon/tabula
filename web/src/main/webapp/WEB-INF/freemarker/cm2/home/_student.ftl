<#import "*/coursework_components.ftl" as components />
<#escape x as x?html>

<h1>Assignments</h1>

<#if nonempty(studentInformation.unsubmittedAssignments) || nonempty(studentInformation.inProgressAssignments) || nonempty(studentInformation.pastAssignments)>
	<@components.student_assignment_list id="todo" title="To do" assignments=studentInformation.unsubmittedAssignments show_submission_progress=true />
	<@components.student_assignment_list id="doing" title="Doing" assignments=studentInformation.inProgressAssignments />
	<@components.student_assignment_list id="done" title="Done" assignments=studentInformation.pastAssignments />
<#else>
	You do not currently have any assignments on Tabula for ${academicYear.toString}.
</#if>

</#escape>