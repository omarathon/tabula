<#escape x as x?html>

<#if !monitoringPointsByTerm??>
	<p><em>There are no monitoring points defined for this academic year.</em></p>
<#else>

<#assign can_record=can.do("MonitoringPoints.Record", command.student) />
<#assign is_the_student=currentUser.apparentUser.warwickId==command.student.universityId />

<#macro pointsInATerm term>
	<#list monitoringPointsByTerm[term] as point>
		<div class="item-info row-fluid term">
			<div class="span12">
				<h4>${term}</h4>
				<div class="row-fluid point">
					<div class="span8 ellipsis" title="${point.name} (<@fmt.monitoringPointFormat point true />)">
						${point.name} (<a class="use-tooltip" data-html="true" title="<@fmt.monitoringPointDateFormat point />"><@fmt.monitoringPointFormat point /></a>)
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
								<@routes.profile command.student />
							</#local>
							<a href="<@routes.recordStudentPoint point command.student returnTo />"
									class="btn btn-mini btn-primary"
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

<div class="monitoring-points-profile striped-section collapsible <#if defaultExpand?? && defaultExpand>expanded</#if>">
	<h3 class="section-title">Monitoring points</h3>
	<div class="missed-info">
		<#if missedCountByTerm?keys?size == 0 && (monitoringPointsByTerm?keys?size > 0) >
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
<script>
	jQuery(function($){
		$('.use-tooltip').tooltip();
	});
</script>
</#escape>