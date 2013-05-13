<#escape x as x?html>
<div id="tutors">
	<h1>Students in ${department.name} with no personal tutor</h1>
	
	<#if studentCount gt 0>
		<#if missingStudents?has_content>
			<table class="tutees table-bordered table-striped table-condensed tabula-greenLight">
				<thead>
					<tr>
						<th class="tutee-col">First name</th>
						<th class="tutee-col">Last name</th>
						<th class="id-col">ID</th>
						<th class="type-col">Type</th>
						<th class="year-col">Year</th>
						<th class="course-col">Course</th>
					</tr>
				</thead>
				
				<tbody>
					<#list missingStudents as student>
						<tr class="tutee">
							<td><h6>${student.firstName}</h6></td>
							<td><h6>${student.lastName}</h6></td>
							<td><a class="profile-link" href="<@routes.profile student />">${student.universityId}</a></td>
							<td>${student.groupName}</td>
							<td>${(student.studyDetails.yearOfStudy)!""}</td>
							<td><#if student.studyDetails.route??>${student.studyDetails.route.name}</#if></td>
						</tr>
					</#list>
				</tbody>
			</table>
		<#else>
			<p class="alert alert-success"><i class="icon-ok"></i> All students in Tabula have personal tutors recorded.</p>
		</#if>
	<#else>
		<p class="alert alert-warning"><i class="icon-warning-sign"></i> No students are currently visible for ${department.name} in Tabula.</p>
	</#if>
</div>

<script type="text/javascript" src="/static/libs/jquery-tablesorter/jquery.tablesorter.min.js"></script>
<script type="text/javascript">
(function($) {
	$(function() {
		$(".tutees").tablesorter({
			sortList: [[1,0], [3,0], [4,0]]
		});
		
		$(".tutee").on("mouseover", function(e) {
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