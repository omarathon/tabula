<#escape x as x?html>
<div id="tutors">
	<h1>Students with no personal tutor</h1>
	
	<#if students?has_content>
		<table class="tutees table-bordered table-striped table-condensed tabula-greenLight">
			<thead>
				<tr>
					<th class="tutee-col">Student</th>
					<th class="type-col">Type</th>
					<th class="year-col">Year</th>
					<th class="course-col">Course</th>
				</tr>
			</thead>
			
			<tbody>
				<#list students as student>
					<tr class="tutee">
						<td>
							<h6><a href="<@routes.profile student />">${student.fullName}</a></h6>
							<span class="muted">${student.universityId}</span>
						</td>
						<td>${student.groupName}</td>
						<td>${student.yearOfStudy}</td>
						<td>${student.route.name}</td>
					</tr>
				</#list>
			</tbody>
		</table>
	<#else>
		<p class="alert alert-warning"><i class="icon-warning-sign"></i> All students in Tabula have personal tutors recorded.</p>
	</#if>
</div>

<script type="text/javascript" src="/static/libs/jquery-tablesorter/jquery.tablesorter.min.js"></script>
<script type="text/javascript">
(function($) {
	$(".tutees").tablesorter({
		sortList: [[1,0], [2,0], [3,0]]
	});
})(jQuery);
</script>
</#escape>