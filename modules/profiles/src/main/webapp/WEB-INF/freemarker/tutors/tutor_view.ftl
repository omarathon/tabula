<#function sanitise text>
	<#return text?lower_case?replace("[^a-z]", "", "r") />
</#function>

<#escape x as x?html>
<div id="tutor-view">
	<h1>Personal tutors for ${department.name}</h1>
	
	<#if studentCount gt 0>
		<#if can.do("Profiles.PersonalTutor.Upload", department)>
			<p>
<!--				<a class="btn" href="<@routes.tutor_upload department />" title="Upload Excel spreadsheet of new tutors"><i class="icon-upload"></i> Upload new tutor spreadsheet</a> -->
			</p>
		</#if>
		
		<#if tutorRelationships?has_content>
			<table id="tutors" class="table table-bordered">
				<#list tutorRelationships?keys?sort as key>
					<#assign tutor = tutorRelationships[key]?first.agentParsed />
					<#assign tutees = tutorRelationships[key] />
					<#assign tuteeKey = sanitise(key) + "-tutees" />
					
					<tbody>
						<tr>
							<td>
								<h4 class="collapse-trigger" id="${tuteeKey}-trigger" data-toggle="collapse" data-target="#${tuteeKey}" title="Expand">
									<span class="tutor-detail pull-right"><@fmt.p tutees?size "tutee" /></span>
									<i class="icon-chevron-right"></i> 
									<#if tutor?is_string>
										${tutor}
										<#if !tutor?string?starts_with("Not ")>
											<span class="tutor-detail">External to Warwick</span>
										</#if>
									<#else>
										${tutor.fullName}
										<#if tutor.homeDepartment.code != department.code>
											<span class="tutor-detail">${tutor.homeDepartment.name}</span>
										</#if>
									</#if>
								</h4>
								
								<div id="${tuteeKey}" class="collapse">
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
											<#list tutees as tuteeRelationship>
												<#assign student = tuteeRelationship.studentMember />
												<tr class="tutee">
													<td><h6>${student.firstName}</h6></td>
													<td><h6>${student.lastName}</h6></td>
													<td><a href="<@routes.profile student />">${student.universityId}</a></td>
													<td>${student.groupName}</td>
													<td>${student.studyDetails.yearOfStudy!""}</td>
													<td>${student.studyDetails.route.name!""}</td>
												</tr>
											</#list>
										</tbody>
									</table>
								</div>
							</td>
						</tr>
					</tbody>
				</#list>
			</table>
		<#else>
			<p class="alert alert-warning"><i class="icon-warning-sign"></i> No personal tutors are currently visible for ${department.name} in Tabula.</p>
		</#if>
		
		<#if missingCount == 0>
			<h4 class="muted"><i class="icon-ok"></i> All students in ${department.name} have personal tutors recorded</h4>
		<#else>
			<h4><a href="<@routes.tutors_missing department />">View <@fmt.p missingCount "student" /> with no personal tutor</a></h4>
		</#if>
	<#else>
		<p class="alert alert-warning"><i class="icon-warning-sign"></i> No students are currently visible for ${department.name} in Tabula.</p>
	</#if>
</div>

<script type="text/javascript" src="/static/libs/jquery-tablesorter/jquery.tablesorter.min.js"></script>
<script type="text/javascript">
(function($) {
	$(".tutees").tablesorter({
		sortList: [[1,0], [3,0], [4,0]]
	});
	
	$("#tutors").on("hidden", "div", function() {
		$("#" + this.id + "-trigger i").removeClass("icon-chevron-down").addClass("icon-chevron-right").parent().prop("title", "Expand");
	}).on("shown", "div", function() {
		$("#" + this.id + "-trigger i").removeClass("icon-chevron-right").addClass("icon-chevron-down").parent().prop("title", "Collapse");
	});
})(jQuery);
</script>
</#escape>