<#escape x as x?html>
<div id="agents">
	<div class="pull-right">
		<#if features.personalTutorAssignment && !relationshipType.readOnly(department)>
			<a href="<@routes.relationship_allocate department relationshipType />" class="btn btn-medium pull-right">
				<i class="icon-random icon-fixed-width"></i> Assign ${relationshipType.description}s</a>
			</a>
		</#if>
	</div>
	
	<h1>Students in ${department.name} with no ${relationshipType.agentRole}</h1>

	<#if studentCount gt 0>
		<#if missingStudents?has_content>
			<table class="students table table-bordered table-striped table-condensed tabula-purple">
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
							<td><a class="profile-link" href="<@routes.profile student />">${student.universityId}</a></td>
							<td>${student.groupName!""}</td>
							<td>
								${(student.mostSignificantCourseDetails.latestStudentCourseYearDetails.yearOfStudy)!}
							</td>
							<td>
								${(student.mostSignificantCourseDetails.route.name)!}
							</td>
						</tr>
					</#list>
				</tbody>
			</table>
			
			<p>
				<@fmt.bulk_email_students students=missingStudents subject="${relationshipType.agentRole?cap_first}" />
			</p>
		<#else>
			<p class="alert alert-success"><i class="icon-ok"></i> All students in ${department.name} have ${relationshipType.agentRole}s recorded.</p>
		</#if>
	<#else>
		<p class="alert alert-warning"><i class="icon-warning-sign"></i> No students are currently visible for ${department.name} in Tabula.</p>
	</#if>
</div>

<script type="text/javascript" src="/static/libs/jquery-tablesorter/jquery.tablesorter.min.js"></script>
<script type="text/javascript">
(function($) {
	$(function() {
		$(".students").tablesorter({
			sortList: [[1,0], [3,0], [4,0]]
		});

		$(".student").on("mouseover", function(e) {
			$(this).find("td").addClass("hover");
		}).on("mouseout", function(e) {
			$(this).find("td").removeClass("hover");
		}).on("click", function(e) {
			if (! $(e.target).is("a")) $(this).find("a.profile-link")[0].click();
		});
	});
})(jQuery);
</script>
</#escape>