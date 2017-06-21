<#import "*/coursework_components.ftl" as components />
<#escape x as x?html>

<h1>Assignments</h1>

<#if !studentInformation.empty>
	<@components.student_assignment_list
		id="student-action"
		title="Awaiting submission"
		assignments=studentInformation.actionRequiredAssignments
		empty_message="You have no assignments that you need to submit in Tabula."
		expand_by_default=true
		show_submission_progress=true
	/>

	<@components.student_assignment_list
		id="student-noaction"
		title="Submitted"
		assignments=studentInformation.noActionRequiredAssignments
		empty_message="You have submitted assignments awaiting feedback in Tabula."
		expand_by_default=(!studentInformation.actionRequiredAssignments?has_content)
	/>

	<@components.student_assignment_list
		id="student-upcoming"
		title="Upcoming"
		assignments=studentInformation.upcomingAssignments
		empty_message="You have no upcoming assignments in Tabula."
		expand_by_default=(!studentInformation.actionRequiredAssignments?has_content && !studentInformation.noActionRequiredAssignments?has_content)
	/>

	<@components.student_assignment_list
		id="student-completed"
		title="Feedback available"
		assignments=studentInformation.completedAssignments
		empty_message="You have no assignments with feedback available in Tabula."
		expand_by_default=(!studentInformation.actionRequiredAssignments?has_content && !studentInformation.noActionRequiredAssignments?has_content && !studentInformation.upcomingAssignments?has_content)
	/>
<#else>
	You do not currently have any assignments on Tabula.
</#if>

</#escape>