<#escape x as x?html>

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
							<span class="passed">Attended</span>
						<#else>
							<span class="missed">Missed</span>
						</#if>
					</div>
				</div>
			</div>
		</div>
	</#list>
</#macro>

<div class="monitoring-points striped-section collapsible">
	<h3 class="section-title">Monitoring points</h3>
	<div class="missed-info">
		<#if command.missedCountByTerm?keys?size == 0 && (command.monitoringPointsByTerm?keys?size > 0) >
			${command.studentCourseDetails.student.firstName} has attended all monitoring points.
		<#else>
			<#list ["Autumn", "Christmas vacation", "Spring", "Easter vacation", "Summer", "Summer vacation"] as term>
				<#if command.missedCountByTerm[term]??>
					<div class="missed">
						<i class="icon-warning-sign"></i> ${command.studentCourseDetails.student.firstName} has missed
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
			<#list ["Autumn", "Christmas vacation", "Spring", "Easter vacation", "Summer", "Summer vacation"] as term>
        		<#if command.monitoringPointsByTerm[term]??>
        			<@pointsInATerm term/>
        		</#if>
        	</#list>
		</#if>
	</div>
</div>

</#escape>