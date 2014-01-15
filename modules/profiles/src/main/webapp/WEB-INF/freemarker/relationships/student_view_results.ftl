<#import "../related_students/related_students_macros.ftl" as student_macros />

<#escape x as x?html>
	<#if students?has_content>

		<@student_macros.table students />

		<p>
			<@fmt.bulk_email_students students=students />
		</p>

	<#else>
		<p class="alert alert-warning"><i class="icon-warning-sign"></i> No ${relationshipType.studentRole}s are currently visible for you in Tabula.</p>
	</#if>

</#escape>
