<#escape x as x?html>
<#include "*/attendance_variables.ftl" />
<#assign can_record=can.do("MonitoringPoints.Record", command.studentCourseDetails) />
<#assign is_the_student=currentUser.apparentUser.warwickId==command.studentCourseDetails.student.universityId />

<#macro pointsInATerm term>
	<#list command.monitoringPointsByTerm[term]?sort_by("week") as point>
		<div class="item-info row-fluid term">
			<div class="span12">
				<h4>${term}</h4>
				<div class="row-fluid point">
					<div class="span8 ellipsis" title="${point.name} (<@fmt.weekRanges point />)">
						${point.name} (<@fmt.weekRanges point />)
					</div>
					<div class="span2 state">
						<#assign checkpointState = (command.checkpointState[point.id]?string("true", "false"))!"null" />
						<#if checkpointState == "null">

						<#elseif checkpointState == "true">
							<span class="label label-success">Attended</span>
						<#else>
							<span class="label label-important">Missed</span>
						</#if>
					</div>
					<div class="span2">
						<#if can_record>
							<#assign returnTo>
								<@routes.profile command.studentCourseDetails.student />
							</#assign>
							<a href="<@url page="/${point.pointSet.route.department.code}/${point.id}/record?returnTo=${returnTo}"/>#student-${command.studentCourseDetails.student.universityId}"
								<#if point.sentToAcademicOffice>
									class="btn btn-mini disabled" title="Monitoring information for this point has been submitted and can no longer be edited"
								<#else>
									class="btn btn-mini btn-primary"
								</#if>
							>
								Record
							</a>
						</#if>
					</div>
				</div>
			</div>
		</div>
	</#list>
</#macro>

<div class="monitoring-points-profile striped-section collapsible <#if defaultExpand??></#if>">
	<h3 class="section-title">Monitoring points</h3>
	<div class="missed-info">
		<#if command.missedCountByTerm?keys?size == 0 && (command.monitoringPointsByTerm?keys?size > 0) >
			<#if is_the_student>
				You have attended all monitoring points.
			<#else>
				${command.studentCourseDetails.student.firstName} has attended all monitoring points.
			</#if>
		<#else>
			<#list monitoringPointTermNames as term>
				<#if command.missedCountByTerm[term]??>
					<div class="missed">
						<i class="icon-warning-sign"></i>
						<#if is_the_student>
							You have
						<#else>
							${command.studentCourseDetails.student.firstName} has
						</#if>
						 missed
						<#if command.missedCountByTerm[term] == 1>
							1 monitoring point
						<#else>
							${command.missedCountByTerm[term]} monitoring points
						</#if>
						in ${term}
					</div>
				</#if>
			</#list>
		</#if>
	</div>

	<div class="striped-section-contents">
		<#if command.monitoringPointsByTerm?keys?size == 0>
			<div class="item-row row-fluid">
				<div class="span12"><em>There are no monitoring points for this route and year of study.</em></div>
			</div>
		<#else>
			<#list monitoringPointTermNames as term>
        		<#if command.monitoringPointsByTerm[term]??>
        			<@pointsInATerm term/>
        		</#if>
        	</#list>
		</#if>
	</div>
</div>

</#escape>