<#escape x as x?html>

	<#import "../attendance_macros.ftl" as attendance_macros />
	<#import "../attendance_variables.ftl" as attendance_variables />

<h1>My ${relationshipType.studentRole}s</h1>

	<#if studentAttendance.totalResults == 0>
	<p><em>No ${relationshipType.studentRole}s were found.</em></p>
	<#else>
		<#assign returnTo><@routes.agentHomeForYear relationshipType academicYear.startYear?c /></#assign>

		<@attendance_macros.scrollablePointsTable
		command=command
		filterResult=studentAttendance
		visiblePeriods=visiblePeriods
		monthNames=monthNames
		department=department
		doCommandSorting=false
		; result>
		<td class="unrecorded">
			<a href="<@routes.agentStudent relationshipType academicYear.startYear?c result.student />">
				<span class="badge badge-<#if (result.checkpointTotal.unrecorded > 2)>important<#elseif (result.checkpointTotal.unrecorded > 0)>warning<#else>success</#if>">
				${result.checkpointTotal.unrecorded}
				</span>
			</a>
		</td>
		<td class="missed">
			<a href="<@routes.agentStudent relationshipType academicYear.startYear?c result.student />">
				<span class="badge badge-<#if (result.checkpointTotal.unauthorised > 2)>important<#elseif (result.checkpointTotal.unauthorised > 0)>warning<#else>success</#if>">
				${result.checkpointTotal.unauthorised}
				</span>
			</a>
		</td>
		<td class="record">
			<#assign record_url><@routes.agentRecord relationshipType academicYear.startYear?c result.student returnTo/></#assign>
			<@fmt.permission_button
			permission='MonitoringPoints.Record'
			scope=result.student
			action_descr='record monitoring points'
			classes='btn btn-primary btn-mini'
			href=record_url
			tooltip='Record'
			>
				<i class="icon-pencil icon-fixed-width late"></i>
			</@fmt.permission_button>
		</td>
		</@attendance_macros.scrollablePointsTable>

	<div class="monitoring-points">
		<#list attendance_variables.monitoringPointTermNames as term>
			<#if groupedPoints[term]??>
				<@attendance_macros.groupedPointsBySection groupedPoints term; groupedPoint>
					<div class="span12">
						<div class="pull-right">
							<#assign record_url><@routes.agentRecordPoints relationshipType academicYear.startYear?c groupedPoint.templatePoint returnTo/></#assign>
							<a href="${record_url}" class="btn btn-primary <#if !canRecordAny>disabled</#if>">Record</a>
						</div>
					${groupedPoint.templatePoint.name}
						(<a class="use-tooltip" data-html="true" title="
							<@fmt.wholeWeekDateFormat
					groupedPoint.templatePoint.startWeek
					groupedPoint.templatePoint.endWeek
					groupedPoint.templatePoint.scheme.academicYear
					/>
						"><@fmt.monitoringPointWeeksFormat
							groupedPoint.templatePoint.startWeek
					groupedPoint.templatePoint.endWeek
					groupedPoint.templatePoint.scheme.academicYear
					department
						/></a>)
						<#assign popoverContent>
							<ul>
								<#list groupedPoint.schemes?sort_by("displayName") as scheme>
									<li>${scheme.displayName}</li>
								</#list>
							</ul>
						</#assign>
						<a href="#" class="use-popover" data-content="${popoverContent}" data-html="true" data-placement="right">
							<@fmt.p groupedPoint.schemes?size "scheme" />
						</a>
					</div>
				</@attendance_macros.groupedPointsBySection>
			</#if>
		</#list>

		<#list monthNames as month>
			<#if groupedPoints[month]??>
				<@attendance_macros.groupedPointsBySection groupedPoints month; groupedPoint>
					<div class="span12">
						<div class="pull-right">
							<#assign record_url><@routes.agentRecordPoints relationshipType academicYear.startYear?c groupedPoint.templatePoint returnTo/></#assign>
							<a href="${record_url}" class="btn btn-primary <#if !canRecordAny>disabled</#if>">Record</a>
						</div>
					${groupedPoint.templatePoint.name}
						(<@fmt.interval groupedPoint.templatePoint.startDate groupedPoint.templatePoint.endDate />)
						<#assign popoverContent>
							<ul>
								<#list groupedPoint.schemes?sort_by("displayName") as scheme>
									<li>${scheme.displayName}</li>
								</#list>
							</ul>
						</#assign>
						<a href="#" class="use-popover" data-content="${popoverContent}" data-html="true" data-placement="right">
							<@fmt.p groupedPoint.schemes?size "scheme" />
						</a>
					</div>
				</@attendance_macros.groupedPointsBySection>
			</#if>
		</#list>

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
				sortList: [[2,0], [1,0]],
				headers: {0:{sorter:false}}
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
				headers: {2:{sorter:false}},
				textExtraction: function(node) {
					var $el = $(node);
					if ($el.data('sortby')) {
						return $el.data('sortby');
					} else {
						return $el.text().trim();
					}
				}
			});
		});
	</script>
</#if>
</#escape>