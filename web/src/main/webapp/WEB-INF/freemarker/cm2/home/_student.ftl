<#import "*/coursework_components.ftl" as components />
<#escape x as x?html>

<h1>Assignments</h1>

<#if !studentInformation.empty>
	<@components.student_assignment_list id="student-action" title="Action required" assignments=studentInformation.actionRequiredAssignments show_submission_progress=true />
	<@components.student_assignment_list id="student-noaction" title="No action required" assignments=studentInformation.noActionRequiredAssignments expand_by_default=(!studentInformation.actionRequiredAssignments?has_content) />
	<@components.student_assignment_list id="student-upcoming" title="Upcoming" assignments=studentInformation.upcomingAssignments expand_by_default=(!studentInformation.actionRequiredAssignments?has_content && !studentInformation.noActionRequiredAssignments?has_content) />
	<@components.student_assignment_list id="student-completed" title="Completed" assignments=studentInformation.completedAssignments expand_by_default=(!studentInformation.actionRequiredAssignments?has_content && !studentInformation.noActionRequiredAssignments?has_content && !studentInformation.upcomingAssignments?has_content) />
<#else>
	You do not currently have any assignments on Tabula for ${academicYear.toString}.
</#if>

</#escape>