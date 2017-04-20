<#import "*/coursework_components.ftl" as components />
<#escape x as x?html>

<h1>Assignments</h1>

	<@components.student_assignment_list id="todo" title="To do" assignments=studentInformation.unsubmittedAssignments show_submission_progress=true />
	<@components.student_assignment_list id="doing" title="Doing" assignments=studentInformation.inProgressAssignments />
	<@components.student_assignment_list id="done" title="Done" assignments=studentInformation.pastAssignments />

</#escape>