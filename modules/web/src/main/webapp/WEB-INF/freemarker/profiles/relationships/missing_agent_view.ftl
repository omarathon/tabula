<#escape x as x?html>
<div>
	<div class="pull-right">
		<#if features.personalTutorAssignment && !relationshipType.readOnly(department)>
			<a href="<@routes.profiles.relationship_allocate department relationshipType />" class="btn btn-default pull-right">
				Allocate ${relationshipType.description}s</a>
			</a>
		</#if>
	</div>

	<h1 class="with-settings">Students in ${department.name} with no ${relationshipType.agentRole}</h1>

	<#if studentCount gt 0>
		<#if missingStudents?has_content>
			<table class="related_students table table-striped table-condensed">
				<thead>
					<tr>
						<th class="student-col">First name</th>
						<th class="student-col">Last name</th>
						<th class="id-col">ID</th>
						<th class="type-col">Type</th>
						<th class="year-col">Year</th>
						<th class="course-col">Course</th>
					</tr>
				</thead>

				<tbody>
					<#list missingStudents as student>
						<tr class="student">
							<td><h6>${student.firstName}</h6></td>
							<td><h6>${student.lastName}</h6></td>
							<td><a class="profile-link" href="<@routes.profiles.profile student />">${student.universityId}</a></td>
							<td>${student.groupName!""}</td>
							<td>
								${(student.mostSignificantCourseDetails.latestStudentCourseYearDetails.yearOfStudy)!}
							</td>
							<td>
								${(student.mostSignificantCourseDetails.currentRoute.name)!}
							</td>
						</tr>
					</#list>
				</tbody>
			</table>

			<p>
				<@fmt.bulk_email_students students=missingStudents subject="${relationshipType.agentRole?cap_first}" />
			</p>
		<#else>
			<p class="alert alert-info">All students in ${department.name} have ${relationshipType.agentRole}s recorded.</p>
		</#if>
	<#else>
		<p class="alert alert-info">No students are currently visible for ${department.name} in Tabula.</p>
	</#if>
</div>

<script type="text/javascript">
(function($) {
	$(function() {
		$('.related_students').tablesorter({
			sortList: [[1,0], [3,0], [4,0]]
		});

		$('.student').on('mouseover', function(e) {
			$(this).find('td').addClass('hover');
		}).on('mouseout', function(e) {
			$(this).find('td').removeClass('hover');
		}).on('click', function(e) {
			if (! $(e.target).is('a')) $(this).find('a.profile-link')[0].click();
		});
	});
})(jQuery);
</script>
</#escape>