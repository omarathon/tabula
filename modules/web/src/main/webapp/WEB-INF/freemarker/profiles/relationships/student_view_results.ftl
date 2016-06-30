<#import "../related_students/related_students_macros.ftl" as student_macros />

<#escape x as x?html>
	<#if studentCourseDetails?has_content>

		<@student_macros.tableWithMeetingsColumn items=studentCourseDetails meetingsMap=meetingsMap/>

		<p><a class="btn btn-default new-meeting-record" href="test">Record meeting for selected students</a> <@fmt.bulk_email_students students=students /></p>

	<#else>
		<p class="alert alert-info">No ${relationshipType.studentRole}s are currently visible for you in Tabula.</p>
	</#if>

</#escape>
