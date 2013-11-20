<#escape x as x?html>

<#import "../attendance_macros.ftl" as attendance_macros />
<#import "../attendance_variables.ftl" as attendance_variables />

<#assign thisPath><@routes.agentView command.relationshipType /></#assign>

<h1>My ${command.relationshipType.studentRole}s</h1>

<#if students?size == 0>
	<p><em>No ${command.relationshipType.studentRole}s were found.</em></p>
<#else>

	<@attendance_macros.academicYearSwitcher thisPath command.academicYear command.thisAcademicYear />

	<div class="scrollable-points-table">
		<div class="row">
			<div class="left">
				<table class="students table table-bordered table-striped table-condensed">
					<thead>
					<tr>
						<th class="student-col">First name</th>
						<th class="student-col">Last name</th>
						<th class="id-col">ID</th>
					</tr>
					</thead>

					<tbody>
						<#list students as studentData>
						<tr class="student">
							<td class="fname" title="${studentData.student.firstName}">${studentData.student.firstName}</td>
							<td class="lname" title="${studentData.student.lastName}">${studentData.student.lastName}</td>
							<td class="id"><a class="profile-link" href="<@routes.profile studentData.student />">${studentData.student.universityId}</a></td>
						</tr>
						</#list>
					</tbody>
				</table>
			</div>

			<div class="middle">
				<table class="attendance table tablesorter table-bordered table-striped table-condensed sb-no-wrapper-table-popout">
					<thead>
					<tr>
						<#list attendance_variables.monitoringPointTermNames as term>
							<#if necessaryTerms?seq_contains(term)>
								<th class="${term}-col">${term}</th>
							</#if>
						</#list>
						<#if necessaryTerms?size == 0>
							<th>&nbsp;</th>
						</#if>
					</tr>
					</thead>

					<tbody>
						<#list students as studentData>
							<tr class="student">
								<#if studentData.pointsByTerm?keys?size == 0>
									<td colspan="${necessaryTerms?size}"><span class="muted"><em>No monitoring points found</em></span></td>
								<#else>
									<#list attendance_variables.monitoringPointTermNames as term>
										<#if necessaryTerms?seq_contains(term)>
											<td>
												<#if studentData.pointsByTerm[term]??>
													<#assign pointMap = studentData.pointsByTerm[term] />
													<#list pointMap?keys?sort_by("validFromWeek") as point>
														<#assign checkpointState = mapGet(pointMap, point) />
														<#if checkpointState == "attended">
															<i class="icon-ok icon-fixed-width attended" title="Attended: ${point.name} (<@fmt.weekRanges point />)"></i>
														<#elseif checkpointState == "authorised">
															<i class="icon-remove-circle icon-fixed-width authorised" title="Missed (authorised): ${point.name} (<@fmt.weekRanges point />)"></i>
														<#elseif checkpointState == "unauthorised">
															<i class="icon-remove icon-fixed-width unauthorised" title="Missed (unauthorised): ${point.name} (<@fmt.weekRanges point />)"></i>
														<#elseif checkpointState == "late">
															<i class="icon-warning-sign icon-fixed-width late" title="Unrecorded: ${point.name} (<@fmt.weekRanges point />)"></i>
														<#else>
															<i class="icon-minus icon-fixed-width" title="${point.name} (<@fmt.weekRanges point />)"></i>
														</#if>
													</#list>
												<#else>
													<i class="icon-fixed-width"></i>
												</#if>
											</td>
										</#if>
									</#list>
								</#if>
							</tr>
						</#list>
					</tbody>
				</table>
			</div>

			<div class="right">
				<table class="counts table table-bordered table-striped table-condensed">
					<thead>
					<tr>
						<th class="unrecorded-col"><i title="Unrecorded" class="icon-warning-sign icon-fixed-width late"></i></th>
						<th class="missed-col" data-field="missedMonitoringPoints"><i title="Missed monitoring points" class="icon-remove icon-fixed-width unauthorised"></i></th>
						<th class="record-col"></th>
					</tr>
					</thead>

					<tbody>
						<#list students as studentData>
							<tr class="student">
								<td class="unrecorded">
									<a href="<@routes.agentStudentView studentData.student command.relationshipType command.academicYear />">
										<span class="badge badge-<#if (studentData.unrecorded > 2)>important<#elseif (studentData.unrecorded > 0)>warning<#else>success</#if>">
											${studentData.unrecorded}
										</span>
									</a>
								</td>
								<td class="missed">
									<a href="<@routes.agentStudentView studentData.student command.relationshipType command.academicYear />">
										<span class="badge badge-<#if (studentData.missed > 2)>important<#elseif (studentData.missed > 0)>warning<#else>success</#if>">
											${studentData.missed}
										</span>
									</a>
								</td>
								<td class="record">
									<#if studentData.pointsByTerm?keys?size == 0>
										<a title="This student has no monitoring points" class="btn btn-primary btn-mini disabled"><i class="icon-pencil icon-fixed-width"></i></a>
									<#else>
										<#assign record_url><@routes.agentStudentRecord studentData.student command.relationshipType command.academicYear thisPath /></#assign>
										<@fmt.permission_button
										permission='MonitoringPoints.Record'
										scope=studentData.student
										action_descr='record monitoring points'
										classes='btn btn-primary btn-mini'
										href=record_url
										>
											<i class="icon-pencil icon-fixed-width late"></i>
										</@fmt.permission_button>
									</#if>
								</td>
							</tr>
						</#list>
					</tbody>
				</table>
			</div>
		</div>
	</div>

	<script>
		jQuery(window).on('load', function(){
			Attendance.scrollablePointsTableSetup();
		});
		jQuery(function($){
			Attendance.tableSortMatching([
				$('.scrollable-points-table .students'),
				$('.scrollable-points-table .attendance'),
				$('.scrollable-points-table .counts')
			]);
			$(".scrollable-points-table .students").tablesorter({
				sortList: [[1,0], [0,0]]
			});
			$(".scrollable-points-table .attendance").tablesorter({
					headers: {
						0:{sorter:false},
						1:{sorter:false},
						2:{sorter:false},
						3:{sorter:false},
						4:{sorter:false},
						5:{sorter:false}
					}
			});
			$(".scrollable-points-table .counts").tablesorter({
				headers: {2:{sorter:false}}
			});
		});
	</script>
</#if>
</#escape>