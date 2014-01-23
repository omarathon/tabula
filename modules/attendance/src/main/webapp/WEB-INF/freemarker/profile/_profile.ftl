<#escape x as x?html>
<#import "../attendance_macros.ftl" as attendance_macros />

<#if !pointsByTerm??>
	<p><em>There are no monitoring points defined for this academic year.</em></p>
<#else>

<#assign can_record=can.do("MonitoringPoints.Record", command.student) />
<#assign is_the_student=currentUser.apparentUser.warwickId==command.student.universityId />

<#macro pointsInATerm term>
	<table class="table">
		<tbody>
			<#local pointMap = pointsByTerm[term] />
			<#list pointMap?keys?sort_by("validFromWeek") as point>
				<tr class="point">
					<td class="point" title="${point.name} (<@fmt.monitoringPointFormat point true />)">
						${point.name} (<a class="use-tooltip" data-html="true" title="<@fmt.monitoringPointDateFormat point />"><@fmt.monitoringPointFormat point /></a>)
					</td>
					<td class="state">
						<@attendance_macros.attendanceLabel pointMap point />
					</td>
					<#if can_record>
						<td>
							<#local returnTo>
								<@routes.profile command.student />
							</#local>
							<a href="<@routes.recordStudentPoint point command.student returnTo />"
									class="btn btn-mini btn-primary"
							>
								Record
							</a>
						</td>
					</#if>
				</tr>
			</#list>
		</tbody>
	</table>
</#macro>

<div class="monitoring-points-profile striped-section collapsible <#if defaultExpand?? && defaultExpand>expanded</#if>">
	<h3 class="section-title">Monitoring points</h3>
	<div class="missed-info">
		<#if missedCountByTerm?keys?size == 0 && (pointsByTerm?keys?size > 0) >
			<#if is_the_student>
				You have missed 0 monitoring points.
			<#else>
				${command.student.firstName} has missed 0 monitoring points.
			</#if>
		<#else>
			<#list ["Autumn", "Christmas vacation", "Spring", "Easter vacation", "Summer", "Summer vacation"] as term>
				<#if missedCountByTerm[term]??>
					<div class="missed">
						<i class="icon-warning-sign"></i>
						<#if is_the_student>
							You have
						<#else>
							${command.student.firstName} has
						</#if>
						 missed
						<#if missedCountByTerm[term] == 1>
							1 monitoring point
						<#else>
							${missedCountByTerm[term]} monitoring points
						</#if>
						in ${term}
					</div>
				</#if>
			</#list>
		</#if>
	</div>

	<div class="striped-section-contents">
		<#if pointsByTerm?keys?size == 0>
			<div class="item-row row-fluid">
				<div class="span12"><em>There are no monitoring points for this route and year of study.</em></div>
			</div>
		<#else>
			<#list ["Autumn", "Christmas vacation", "Spring", "Easter vacation", "Summer", "Summer vacation"] as term>
        		<#if pointsByTerm[term]??>
					<div class="item-info row-fluid term">
						<div class="span12">
							<h4>${term}</h4>
		        			<@pointsInATerm term/>
						</div>
					</div>
        		</#if>
        	</#list>
		</#if>
	</div>
</div>

</#if>
</#escape>