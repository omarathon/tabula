<#function sanitise text>
	<#return text?lower_case?replace("[^a-z]", "", "r") />
</#function>

<#escape x as x?html>
<div id="tutor-view">
	<h1>Personal tutors for ${department.name}</h1>
	
	<#if can.do("Profiles.PersonalTutor.Upload", department)>
		<p>
			<a class="btn" href="<@routes.tutor_upload department />" title="Upload Excel spreadsheet of new tutors"><i class="icon-upload"></i> Upload new tutor spreadsheet</a>
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
							<span class="h4-aside muted"><@fmt.p tutees?size "tutee" /></span>
							<h4 class="collapse-trigger" id="${tuteeKey}-trigger" data-toggle="collapse" data-target="#${tuteeKey}" title="Expand"><i class="icon-chevron-right"></i> 
							<#if tutor?is_string>
								${tutor}</h4>
								<#if !tutor?string?starts_with("Not ")>
									<div class="tutor-detail">External to Warwick</div>
								</#if>
							<#else>
								${tutor.fullName}</h4>
								<#if tutor.homeDepartment.code != department.code>
									<div class="tutor-detail">${tutor.homeDepartment.name}</div>
								</#if>
							</#if>
							
							<div id="${tuteeKey}" class="collapse">
								<table class="tutees table-bordered table-striped table-condensed tabula-greenLight">
									<thead>
										<tr>
											<th class="tutee-col">Tutee</th>
											<th class="type-col">Type</th>
											<th class="year-col">Year</th>
											<th class="course-col">Course</th>
										</tr>
									</thead>
									
									<tbody>
										<#list tutees as tuteeRelationship>
											<#assign student = tuteeRelationship.studentMember />
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
							</div>
						</td>
					</tr>
				</tbody>
			</#list>
		</table>
	<#else>
		<p class="alert alert-warning"><i class="icon-warning-sign"></i> No personal tutors are currently visible for ${department.name} in Tabula.</p>
	</#if>
	
	<h4><a href="<@routes.tutors_missing department />">View <@fmt.p missingCount "student" /> with no personal tutor</a></h4>
</div>

<script type="text/javascript" src="/static/libs/jquery-tablesorter/jquery.tablesorter.min.js"></script>
<script type="text/javascript">
(function($) {
	$(".tutees").tablesorter({
		sortList: [[1,0], [2,0], [3,0]]
	});
	
	$("#tutors").on("hidden", "div", function() {
		$("#" + this.id + "-trigger i").removeClass("icon-chevron-down").addClass("icon-chevron-right").parent().prop("title", "Expand");
	}).on("shown", "div", function() {
		$("#" + this.id + "-trigger i").removeClass("icon-chevron-right").addClass("icon-chevron-down").parent().prop("title", "Collapse");
	});
})(jQuery);
</script>
</#escape>