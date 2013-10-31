<#escape x as x?html>

<#if !monitoringPointsByTerm??>
	<p><em>There are no monitoring points defined for this academic year.</em></p>
<#else>

<#assign can_record=can.do("MonitoringPoints.Record", command.studentCourseDetails) />
<#assign is_the_student=currentUser.apparentUser.warwickId==command.studentCourseDetails.student.universityId />

<#macro pointsInATerm term>
	<#list monitoringPointsByTerm[term] as point>
		<div class="item-info row-fluid term">
			<div class="span12">
				<h4>${term}</h4>
				<div class="row-fluid point">
					<div class="span8 ellipsis" title="${point.name} (<@fmt.weekRanges point />)">
						${point.name} (<@fmt.weekRanges point />)
					</div>
					<div class="span2 state">
						<#if checkpointState[point.id]??>
							<#local thisPointCheckpointState = checkpointState[point.id] />
							<#if thisPointCheckpointState == "attended">
								<span class="label label-success">Attended</span>
							<#elseif thisPointCheckpointState == "authorised">
								<span class="label label-info" title="Missed (authorised)">Missed</span>
							<#elseif thisPointCheckpointState == "unauthorised">
								<span class="label label-important" title="Missed (unauthorised)">Missed</span>
							</#if>
						</#if>
					</div>
					<div class="span2">
						<#if can_record>
							<#local returnTo>
								<@routes.profile command.studentCourseDetails.student />
							</#local>
							<a href="<@routes.recordStudent point command.studentCourseDetails returnTo />"
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

<div class="monitoring-points-profile striped-section collapsible <#if defaultExpand??>expanded</#if>">
	<h3 class="section-title">Monitoring points</h3>
	<div class="missed-info">
		<#if missedCountByTerm?keys?size == 0 && (monitoringPointsByTerm?keys?size > 0) >
			<#if is_the_student>
				You have missed 0 monitoring points.
			<#else>
				${command.studentCourseDetails.student.firstName} has missed 0 monitoring points.
			</#if>
		<#else>
			<#list ["Autumn", "Christmas vacation", "Spring", "Easter vacation", "Summer", "Summer vacation"] as term>
				<#if missedCountByTerm[term]??>
					<div class="missed">
						<i class="icon-warning-sign"></i>
						<#if is_the_student>
							You have
						<#else>
							${command.studentCourseDetails.student.firstName} has
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
		<#if monitoringPointsByTerm?keys?size == 0>
			<div class="item-row row-fluid">
				<div class="span12"><em>There are no monitoring points for this route and year of study.</em></div>
			</div>
		<#else>
			<#list ["Autumn", "Christmas vacation", "Spring", "Easter vacation", "Summer", "Summer vacation"] as term>
        		<#if monitoringPointsByTerm[term]??>
        			<@pointsInATerm term/>
        		</#if>
        	</#list>
		</#if>
	</div>
</div>

</#if>

</#escape>