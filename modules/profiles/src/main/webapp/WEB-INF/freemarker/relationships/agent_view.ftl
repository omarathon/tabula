<#function sanitise text>
	<#return text?lower_case?replace("[^a-z]", "", "r") />
</#function>

<#escape x as x?html>
<div id="agent-view">
	<div class="pull-right">
		<#if features.personalTutorAssignment>
			<a href="<@routes.relationship_allocate department relationshipType />" class="btn btn-medium pull-right">
				<i class="icon-random icon-fixed-width"></i> Assign ${relationshipType.description}s</a>
			</a>
		</#if>
	</div>

	<h1>${relationshipType.description}s for ${department.name}</h1>

	<#if studentCount gt 0>
		<#if agentRelationships?has_content>
			<table id="agents" class="table table-bordered">
				<#list agentRelationships?keys?sort as key>
					<#assign agent = agentRelationships[key]?first.agentParsed />
					<#assign students = agentRelationships[key] />
					<#assign studentKey = sanitise(key) + "-students" />

					<tbody>
						<tr>
							<td>
								<h4 class="collapse-trigger" id="${studentKey}-trigger" data-toggle="collapse" data-target="#${studentKey}" title="Expand">
									<span class="agent-detail pull-right"><@fmt.p students?size "${relationshipType.studentRole}" /></span>
									<i class="icon-chevron-right icon-fixed-width"></i>
									<#if agent?is_string>
										${agent}
										<#if !agent?string?starts_with("Not ")>
											<span class="agent-detail">External to Warwick</span>
										</#if>
									<#else>
										${agent.fullName}
										<#if agent.homeDepartment.code != department.code>
											<span class="agent-detail">${agent.homeDepartment.name}</span>
										</#if>
									</#if>
								</h4>

								<div id="${studentKey}" class="collapse">
									<table class="students table-bordered table-striped table-condensed tabula-purple">
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
											<#list students as studentRelationship>
												<#assign student = studentRelationship.studentMember />
												<tr class="student">
													<td><h6>${student.firstName}</h6></td>
													<td><h6>${student.lastName}</h6></td>
													<td><a class="profile-link" href="<@routes.profile student />">${student.universityId}</a></td>
													<td>${student.groupName}</td>
													<td>
														${(student.mostSignificantCourseDetails.latestStudentCourseYearDetails.yearOfStudy)!}
													</td>
													<td>
														${(student.mostSignificantCourseDetails.route.name)!""}
													</td>
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
			<p class="alert alert-warning"><i class="icon-warning-sign"></i> No ${relationshipType.agentRole}s are currently visible for ${department.name} in Tabula.</p>
		</#if>

		<#if missingCount == 0>
			<h4 class="muted"><i class="icon-ok"></i> All students in ${department.name} have ${relationshipType.agentRole}s recorded</h4>
		<#else>
			<h4><a href="<@routes.relationship_missing department relationshipType />">View <@fmt.p missingCount "student" /> with no ${relationshipType.agentRole}</a></h4>
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

		$("#agents").on("hidden", "div", function() {
			$("#" + this.id + "-trigger i").removeClass("icon-chevron-down").addClass("icon-chevron-right").parent().prop("title", "Expand");
		}).on("shown", "div", function() {
			$("#" + this.id + "-trigger i").removeClass("icon-chevron-right").addClass("icon-chevron-down").parent().prop("title", "Collapse");
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