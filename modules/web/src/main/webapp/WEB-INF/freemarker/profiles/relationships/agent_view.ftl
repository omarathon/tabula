<#function sanitise text>
	<#return text?lower_case?replace("[^a-z0-9]", "", "r") />
</#function>

<#escape x as x?html>
<div class="pull-right">
	<#if features.personalTutorAssignment && !relationshipType.readOnly(department)>
		<a href="<@routes.profiles.relationship_allocate department relationshipType />" class="btn btn-default pull-right">
			Allocate ${relationshipType.description}s
		</a>
	</#if>
</div>

<h1 class="with-settings">${relationshipType.description}s for ${department.name}</h1>

<#if agentRelationships?has_content || (missingCount > 0)>
	<#if agentRelationships?has_content>
		<table id="agents" class="table table-bordered">
			<#list agentRelationships?keys as key>
				<#assign students = mapGet(agentRelationships,key) />
				<#assign agent = students?first.agent /><#-- always a string. -->
				<#assign agentMember = students?first.agentMember />
				<#assign studentKey = "rel-" + sanitise(key.sortkey) + "-students" />

				<tbody>
					<tr>
						<td>
							<h4 class="collapse-trigger" id="${studentKey}-trigger" data-toggle="collapse" data-target="#${studentKey}" title="Expand">
								<span class="very-subtle pull-right"><@fmt.p students?size "${relationshipType.studentRole}" /></span>
								<i class="fa fa-chevron-right fa-fw"></i>
								<#if agentMember??>
									${agentMember.fullName}
									<#if agentMember.homeDepartment.code != department.code>
										<span class="very-subtle">${agentMember.homeDepartment.name}</span>
									</#if>
									<#assign agentId = agentMember.universityId />
								<#else>
									${agent}
									<#if !agent?starts_with("Not ")>
										<span class="very-subtle">External to Warwick</span>
									</#if>
									<#assign agentId = agent />
								</#if>
							</h4>

							<div id="${studentKey}" class="collapse">
								<form action="<@routes.profiles.relationship_reallocate department relationshipType agentId />" method="post">
									<table class="related_students table table-striped table-condensed">
										<thead>
											<tr>
												<th><@bs3form.selector_check_all /></th>
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
												<#assign studentCourseDetails = studentRelationship.studentCourseDetails />
												<tr class="student">
													<td><@bs3form.selector_check_row name="preselectStudents" value="${studentCourseDetails.student.universityId}" /></td>
													<td><h6>${studentCourseDetails.student.firstName}</h6></td>
													<td><h6>${studentCourseDetails.student.lastName}</h6></td>
													<td><a class="profile-link" href="/profiles/view/course/${studentCourseDetails.urlSafeId}">${studentCourseDetails.student.universityId}</a></td>
													<td>${studentCourseDetails.student.groupName!""}</td>
													<td>${(mapGet(yearOfStudyMap, studentCourseDetails))!""}</td>
													<td>${(mapGet(courseMap, studentCourseDetails).name)!""}</td>
												</tr>
											</#list>
										</tbody>
									</table>

									<p>
										<button type="submit" class="btn btn-primary">Reallocate students</button>
										<@fmt.bulk_email_student_relationships relationships=students subject="${relationshipType.agentRole?cap_first}" />
									</p>
								</form>
							</div>
						</td>
					</tr>
				</tbody>
			</#list>
		</table>
	<#else>
		<p class="alert alert-info">No ${relationshipType.agentRole}s are currently visible for ${department.name} in Tabula.</p>
	</#if>

	<#if missingCount == 0>
		<h4 class="subtle">All students in ${department.name} have ${relationshipType.agentRole}s recorded</h4>
	<#else>
		<h4><a href="<@routes.profiles.relationship_missing department relationshipType />">View <@fmt.p missingCount "student" /> with no ${relationshipType.agentRole}</a></h4>
	</#if>
<#else>
	<p class="alert alert-info">No students are currently visible for ${department.name} in Tabula.</p>
</#if>

<script type="text/javascript">
	(function($) {
		$(function() {
			$('.related_students').tablesorter({
				sortList: [[2,0], [1,0], [3,0]],
				headers: { 0: { sorter: false} }
			}).bigList();
	
			$('#agents').on('hidden.bs.collapse', 'div', function() {
				$('#' + this.id + '-trigger i').removeClass('fa-chevron-down').addClass('fa-chevron-right').parent().prop('title', 'Expand');
			}).on('shown.bs.collapse', 'div', function() {
				$('#' + this.id + '-trigger i').removeClass('fa-chevron-right').addClass('fa-chevron-down').parent().prop('title', 'Collapse');
			});
		});
	})(jQuery);
</script>
</#escape>