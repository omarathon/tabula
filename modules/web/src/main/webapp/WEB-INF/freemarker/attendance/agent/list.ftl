<#escape x as x?html>

<#import "../attendance_macros.ftl" as attendance_macros />
<#import "../attendance_variables.ftl" as attendance_variables />

<div class="pull-right">
	<@fmt.bulk_email_students students=studentAttendance.students />
</div>

<h1 class="with-settings">My ${relationshipType.studentRole}s</h1>

<#if studentAttendance.totalResults == 0>
	<p><em>No ${relationshipType.studentRole}s were found.</em></p>
<#else>
	<#assign returnTo><@routes.attendance.agentHomeForYear relationshipType academicYear.startYear?c /></#assign>

	<@attendance_macros.id7ScrollablePointsTable
		command=command
		filterResult=studentAttendance
		visiblePeriods=visiblePeriods
		monthNames=monthNames
		department=department
		doCommandSorting=false
	; result>
		<td class="unrecorded">
			<a href="<@routes.attendance.agentStudent relationshipType academicYear.startYear?c result.student />" title="<@attendance_macros.checkpointTotalTitle result.checkpointTotal />" class="use-tooltip">
				<span class="<#if (result.checkpointTotal.unrecorded > 2)>badge progress-bar-danger<#elseif (result.checkpointTotal.unrecorded > 0)>badge progress-bar-warning</#if>">
					${result.checkpointTotal.unrecorded}
				</span>
			</a>
		</td>
		<td class="missed">
			<a href="<@routes.attendance.agentStudent relationshipType academicYear.startYear?c result.student />" title="<@attendance_macros.checkpointTotalTitle result.checkpointTotal />" class="use-tooltip">
				<span class="<#if (result.checkpointTotal.unauthorised > 2)>badge progress-bar-danger<#elseif (result.checkpointTotal.unauthorised > 0)>badge progress-bar-warning</#if>">
					${result.checkpointTotal.unauthorised}
				</span>
			</a>
		</td>
		<td class="record">
			<#assign record_url><@routes.attendance.agentRecord relationshipType academicYear.startYear?c result.student returnTo/></#assign>
				<@fmt.permission_button
				permission='MonitoringPoints.Record'
				scope=result.student
				action_descr='record monitoring points'
				classes='btn btn-primary btn-xs'
				href=record_url
				tooltip='Record'
			>
				Record
			</@fmt.permission_button>
		</td>
	</@attendance_macros.id7ScrollablePointsTable>

	<div class="monitoring-points">
		<#list attendance_variables.monitoringPointTermNames as term>
			<#if groupedPoints[term]??>
				<@attendance_macros.id7GroupedPointsBySection groupedPoints term; groupedPoint>
					<div class="col-md-12">
						<div class="pull-right">
							<#assign record_url><@routes.attendance.agentRecordPoints relationshipType academicYear.startYear?c groupedPoint.templatePoint returnTo/></#assign>
							<a href="${record_url}" class="btn btn-primary btn-sm <#if !canRecordAny>disabled</#if>">Record</a>
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
				</@attendance_macros.id7GroupedPointsBySection>
			</#if>
		</#list>

		<#list monthNames as month>
			<#if groupedPoints[month]??>
				<@attendance_macros.id7GroupedPointsBySection groupedPoints month; groupedPoint>
					<div class="col-md-12">
						<div class="pull-right">
							<#assign record_url><@routes.attendance.agentRecordPoints relationshipType academicYear.startYear?c groupedPoint.templatePoint returnTo/></#assign>
							<a href="${record_url}" class="btn btn-primary btn-sm <#if !canRecordAny>disabled</#if>">Record</a>
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
				</@attendance_macros.id7GroupedPointsBySection>
			</#if>
		</#list>

	</div>

	<script>
		jQuery(window).on('load', function(){
			GlobalScripts.scrollableTableSetup();
		});
		jQuery(function($){
			var $leftTable = $('.scrollable-points-table .left table');
			GlobalScripts.tableSortMatching([
				$leftTable,
				$('.scrollable-points-table .right table')
			]);
			$leftTable.tablesorter({
				sortList: [[2,0], [1,0]],
				headers: {0:{sorter:false},6:{sorter:false}},
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