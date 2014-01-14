<#import "../related_students/related_students_macros.ftl" as student_macros />

<#escape x as x?html>

	<#if students?has_content>
	<table class="related_students table table-bordered table-striped table-condensed tabula-purple">
		<thead>
		<tr>
			<th class="photo-col">Photo</th>
			<th class="student-col">First name</th>
			<th class="student-col">Last name</th>
			<th class="id-col">ID</th>
			<th class="type-col">Type</th>
			<th class="year-col">Year</th>
			<th class="course-but-photo-col">Course</th>
		</tr>
		</thead>

		<tbody>
			<#list students as student>
					<@student_macros.row student />
				</#list>
		</tbody>
	</table>

	<p>
		<@fmt.bulk_email_students students=students />
	</p>

	<#else>
	<p class="alert alert-warning"><i class="icon-warning-sign"></i> No ${relationshipType.studentRole}s are currently visible for you in Tabula.</p>
	</#if>

</#escape>
