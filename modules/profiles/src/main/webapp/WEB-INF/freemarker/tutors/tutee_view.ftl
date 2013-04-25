<#escape x as x?html>
<div id="tutors">
	<h1>My personal tutees</h1>
	
	<#if tutees?has_content>
		<table class="tutees table-bordered table-striped table-condensed tabula-greenLight">
			<thead>
				<tr>
					<th class="photo-col">Photo</th>
					<th class="tutee-col">First name</th>
					<th class="tutee-col">Last name</th>
					<th class="id-col">ID</th>
					<th class="type-col">Type</th>
					<th class="year-col">Year</th>
					<th class="course-but-photo-col">Course</th>
				</tr>
			</thead>
			
			<tbody>
				<#list tutees as tuteeRelationship>
					<#if tuteeRelationship.studentMember?has_content>
						<#assign student = tuteeRelationship.studentMember />
						<tr class="tutee">
							<td>
								<div class="photo">
									<img src="<@routes.photo student />" />
								</div>
							</td>
							<td><h6>${student.firstName}</h6></td>
							<td><h6>${student.lastName}</h6></td>
							<td><a class="profile-link" href="<@routes.profile student />">${student.universityId}</a></td>
							<td>${student.groupName}</td>
							<td>${student.studyDetails.yearOfStudy!""}</td>
							<td>${student.studyDetails.route.name!""}</td>
						</tr>
					</#if>
				</#list>
			</tbody>
		</table>
	<#else>
		<p class="alert alert-warning"><i class="icon-warning-sign"></i> No personal tutees are currently visible for you in Tabula.</p>
	</#if>
</div>

<script type="text/javascript" src="/static/libs/jquery-tablesorter/jquery.tablesorter.min.js"></script>
<script type="text/javascript">
(function($) {
	$(function() {
		$(".tutees").tablesorter({
			sortList: [[2,0], [4,0], [5,0]]
		});
		
		$(".tutee").on("mouseover", function(e) {
			$(this).find("td").addClass("hover");
		}).on("mouseout", function(e) {
			$(this).find("td").removeClass("hover");
		}).on("click", function(e) {
			if (! $(e.target).is("a") && ! $(e.target).is("img")) $(this).find("a.profile-link")[0].click();
		});
	});
})(jQuery);
</script>
</#escape>