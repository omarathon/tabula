<#escape x as x?html>
	<#import "../attendance_variables.ftl" as attendance_variables />

	<#function sortClass field>
		<#list command.sortOrder as order>
			<#if order.propertyName == field>
				<#if order.ascending>
					<#return "headerSortDown" />
				<#else>
					<#return "headerSortUp" />
				</#if>
			</#if>
		</#list>
		<#return "" />
	</#function>

	<#macro pagination currentPage totalResults resultsPerPage extra_classes="">
		<#local totalPages = (totalResults / resultsPerPage)?ceiling />
		<div class="pagination pagination-right ${extra_classes}">
			<ul>
				<#if currentPage lte 1>
					<li class="disabled"><span>&laquo;</span></li>
				<#else>
					<li><a href="?page=${currentPage - 1}" data-page="${currentPage - 1}">&laquo;</a></li>
				</#if>

				<#list 1..totalPages as page>
					<#if page == currentPage>
						<li class="active"><span>${page}</span></li>
					<#else>
						<li><a href="?page=${page}" data-page="${page}">${page}</a></li>
					</#if>
				</#list>

				<#if currentPage gte totalPages>
					<li class="disabled"><span>&raquo;</span></li>
				<#else>
					<li><a href="?page=${currentPage + 1}" data-page="${currentPage + 1}">&raquo;</a></li>
				</#if>
			</ul>
		</div>
	</#macro>

	<#if totalResults?? && students??>
		<#assign filterQuery = command.serializeFilter />
		<#assign returnTo><@routes.viewDepartmentStudentsWithAcademicYear command.department command.academicYear filterQuery/></#assign>
		<#if (totalResults > 0)>
			<div class="clearfix">
				<#if (totalResults > command.studentsPerPage)>
					<div class="pull-right">
						<@pagination command.page totalResults command.studentsPerPage "pagination-small" />
					</div>
				</#if>

				<#assign startIndex = ((command.page - 1) * command.studentsPerPage) />
				<#assign endIndex = startIndex + students?size />
				<p>Results ${startIndex + 1} - ${endIndex} of ${totalResults}</p>
			</div>

			<div class="scrollable-points-table">
				<div class="row">
					<div class="left">
						<table class="table table-bordered table-striped table-condensed">
							<thead>
							<tr>
								<th class="student-col ${sortClass("firstName")} sortable" data-field="firstName">First name</th>
								<th class="student-col ${sortClass("lastName")} sortable" data-field="lastName">Last name</th>
								<th class="id-col ${sortClass("universityId")} sortable" data-field="universityId">ID</th>
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
						<table class="table tablesorter table-bordered table-striped table-condensed sb-no-wrapper-table-popout">
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
																	<i class="use-tooltip icon-ok icon-fixed-width attended" title="Attended: ${point.name} (<@fmt.monitoringPointFormat point true />)" data-container="body"></i>
																<#elseif checkpointState == "authorised">
																	<i class="use-tooltip icon-remove-circle icon-fixed-width authorised" title="Missed (authorised): ${point.name} (<@fmt.monitoringPointFormat point true />)" data-container="body"></i>
																<#elseif checkpointState == "unauthorised">
																	<i class="use-tooltip icon-remove icon-fixed-width unauthorised" title="Missed (unauthorised): ${point.name} (<@fmt.monitoringPointFormat point true />)" data-container="body"></i>
																<#elseif checkpointState == "late">
																	<i class="use-tooltip icon-warning-sign icon-fixed-width late" title="Unrecorded: ${point.name} (<@fmt.monitoringPointFormat point true />)" data-container="body"></i>
																<#else>
																	<i class="use-tooltip icon-minus icon-fixed-width" title="${point.name} (<@fmt.monitoringPointFormat point true />)" data-container="body"></i>
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
						<table class="table table-bordered table-striped table-condensed">
							<thead>
							<tr>
								<th class="unrecorded-col ${sortClass("unrecordedMonitoringPoints")} sortable" data-field="unrecordedMonitoringPoints"><i title="Unrecorded" class="icon-warning-sign icon-fixed-width late"></i></th>
								<th class="missed-col ${sortClass("missedMonitoringPoints")} sortable" data-field="missedMonitoringPoints"><i title="Missed monitoring points" class="icon-remove icon-fixed-width unauthorised"></i></th>
								<th class="record-col"></th>
							</tr>
							</thead>

							<tbody>
								<#list students as studentData>
									<tr class="student">
									<#if studentData.pointsByTerm?keys?size == 0>
										<td colspan="3">&nbsp;</td>
									<#else>
										<td class="unrecorded">
											<a href="<@routes.viewStudent command.department studentData.student command.academicYear />">
												<span class="badge badge-<#if (studentData.unrecorded > 2)>important<#elseif (studentData.unrecorded > 0)>warning<#else>success</#if>">
													${studentData.unrecorded}
												</span>
											</a>
										</td>
										<td class="missed">
											<a href="<@routes.viewStudent command.department studentData.student command.academicYear />">
												<span class="badge badge-<#if (studentData.missed > 2)>important<#elseif (studentData.missed > 0)>warning<#else>success</#if>">
													${studentData.missed}
												</span>
											</a>
										</td>
										<td class="record">
											<#assign record_url><@routes.recordStudent command.department studentData.student command.academicYear returnTo /></#assign>
											<@fmt.permission_button
												permission='MonitoringPoints.Record'
												scope=studentData.student
												action_descr='record monitoring points'
												classes='btn btn-primary btn-mini'
												href=record_url
											>
												<i class="icon-pencil icon-fixed-width late"></i>
											</@fmt.permission_button>
										</td>
									</#if>
									</tr>
								</#list>
							</tbody>
						</table>
					</div>
				</div>
			</div>

			<#if !student_table_script_included??>
				<script type="text/javascript">
					(function($) {
						$(function() {
							$(".scrollable-points-table .left table, .scrollable-points-table .right table").addClass('tablesorter')
								.find('th.sortable').addClass('header')
								.on('click', function(e) {
									var $th = $(this)
										, sortDescending = function(){
											$('#sortOrder').val('desc(' + $th.data('field') + ')');
											$th.closest('thead').find('th').removeClass('headerSortUp').removeClass('headerSortDown');
											$th.addClass('headerSortUp');
										}, sortAscending = function(){
											$('#sortOrder').val('asc(' + $th.data('field') + ')');
											$th.closest('thead').find('th').removeClass('headerSortUp').removeClass('headerSortDown');
											$th.addClass('headerSortDown');
										};

									if ($th.hasClass('headerSortUp')) {
										sortAscending();
									} else if ($th.hasClass('headerSortDown')) {
										sortDescending();
									} else {
										// not currently sorted on this column, default sort depends on column
										if ($th.hasClass('unrecorded-col') || $th.hasClass('missed-col')) {
											sortDescending();
										} else {
											sortAscending();
										}
									}

									if (typeof(window.doRequest) === 'function') {
										window.doRequest($('#command'), true);
									} else {
										$('#command').submit();
									}
								});
						});
						$(window).on('load', function(){
							Attendance.scrollablePointsTableSetup();
						});
					})(jQuery);
				</script>
				<#assign student_table_script_included=true />
			</#if>

			<div class="clearfix">
				<#if totalResults lte command.studentsPerPage>
					<div class="pull-left">
						<@fmt.bulk_email_students students=students />
					</div>
				<#else>
					<@pagination command.page totalResults command.studentsPerPage />
				</#if>
			</div>

		<#else>
			<p>No students were found.</p>
		</#if>
	</#if>

	<script type="text/javascript">
		jQuery(function($) {
			$('.pagination a').on('click', function(e) {
				e.preventDefault();
				e.stopPropagation();

				var page = $(this).data('page');
				$('#page').val(page);

				if (typeof(window.doRequest) === 'function') {
					window.doRequest($('#command'), true);
				} else {
					$('#command').submit();
				}
			});
		});
	</script>

</#escape>