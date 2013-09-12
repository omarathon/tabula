<#escape x as x?html>
<h1>View monitoring points for ${command.dept.name}</h1>

<#if updatedPoint??>
	<div class="alert alert-success">
		<button type="button" class="close" data-dismiss="alert">&times;</button>
		Attendance recorded for '${updatedPoint.name}'
	</div>
</#if>

<form class="form-inline" action="<@url page="/${command.dept.code}"/>">
	<label>Academic year
		<select name="academicYear">
			<#assign academicYears = [command.thisAcademicYear.previous.toString, command.thisAcademicYear.toString, command.thisAcademicYear.next.toString] />
			<#list academicYears as year>
				<option <#if command.academicYear.toString == year>selected</#if> value="${year}">${year}</option>
			</#list>
		</select>
	</label>
	<button type="submit" class="btn btn-primary">Change</button>
</form>

<#if command.setsByRouteByAcademicYear?keys?size == 0>

	<p><em>There are no monitoring point schemes for ${command.dept.name}</em></p>

<#elseif !command.setsByRouteByAcademicYear[command.academicYear.toString]??>

	<p><em>There are no monitoring point schemes for ${command.dept.name} for the selected academic year</em></p>

<#else>

	<form id="viewChooseSet" class="form-inline" action="<@url page="/${command.dept.code}"/>">

		<input type="hidden" value="${command.academicYear.toString}" name="academicYear" />

		<select name="route" class="route input-xxlarge">
			<option style="display:none;" disabled <#if !command.route??>selected</#if> value="">Route</option>
			<#list command.setsByRouteByAcademicYear[command.academicYear.toString]?keys?sort_by("code") as route>
				<option value="${route.code}" <#if command.route?? && command.route.code == route.code>selected</#if>>
					${route.code?upper_case} ${route.name}
				</option>
			</#list>
		</select>

		<select name="set" class="input-medium copy">
			<option style="display:none;" disabled <#if !command.route?? || !command.pointSet??>selected</#if> value="">Year of study</option>
			<#if command.route?? && command.pointSet??>
				<#list command.setsByRouteCodeByAcademicYear(command.academicYear.toString, command.route) as set>
					<option value="${set.id}" <#if command.pointSet.id == set.id>selected</#if>>
						<#if set.year??>${set.year}<#else>All</#if>
					</option>
				</#list>
			</#if>
		</select>

		<button type="submit" class="btn btn-primary">View</button>

		<#assign popoverContent>
			<p>Select a route and year of study from the drop down lists to view the relevant attendance monitoring points.</p>
			<p>If the monitoring points displayed are not correct please contact <a href="mailto:webteam@warwick.ac.uk">webteam@warwick.ac.uk</a></p>
		</#assign>
		<a class="use-popover" id="popover-choose-set"
			data-title="View attendance monitoring points"
			data-content="${popoverContent}"
			data-html="true"
		>
			<i class="icon-question-sign"></i>
		</a>
	</form>

</#if>

<#if command.pointSet??>
	<h2>Students who have missed monitoring points</h2>

	<#if command.membersWithMissedCheckpoints?keys?size == 0>
		<p>There are no students in ${command.route.code?upper_case} ${command.route.name} who have missed monitoring points</p>
	<#else>

		<table id="missed-monitoring-points" class="table table-striped table-bordered table-condensed">
			<thead>
				<tr>
					<th class="sortable">First name</th>
					<th class="sortable">Last name</th>
					<#list ["Autumn", "Christmas vacation", "Spring", "Easter vacation", "Summer", "Summer vacation"] as term>
						<#if command.monitoringPointsByTerm[term]??>
							<th>${term}</th>
						</#if>
					</#list>
					<th class="sortable">Total</th>
				</tr>
			</thead>
			<tbody>
				<#list command.membersWithMissedCheckpoints?keys as member>
					<#assign missedCount = 0 />
					<tr>
						<td>${member.firstName}</td>
						<td>${member.lastName}</td>
						<#list ["Autumn", "Christmas vacation", "Spring", "Easter vacation", "Summer", "Summer vacation"] as term>
							<#if command.monitoringPointsByTerm[term]??>
								<td>
									<#list command.monitoringPointsByTerm[term]?sort_by("week") as point>
										<#assign checkpointState = (command.missedCheckpointsByMemberByPoint(member, point)?string("true", "false"))!"null" />
										<#if checkpointState == "null">
											<i class="icon-minus icon-fixed-width" title="${point.name} (<@fmt.weekRanges point />)"></i>
										<#elseif checkpointState == "true">
											<i class="icon-ok icon-fixed-width" title="${point.name} (<@fmt.weekRanges point />)"></i>
										<#else>
											<#assign missedCount = missedCount + 1 />
											<i class="icon-remove icon-fixed-width" title="${point.name} (<@fmt.weekRanges point />)"></i>
										</#if>
									</#list>
								</td>
							</#if>
						</#list>
						<td>
							<span class="badge badge-<#if (missedCount > 2)>important<#else>warning</#if>">${missedCount}</span>
						</td>
					</tr>
				</#list>
			<tbody>
		</table>

	</#if>

	<h2>Monitoring points for ${command.route.code?upper_case} ${command.route.name},
		<#if command.pointSet.year??>year ${command.pointSet.year}<#else>all years</#if>
	</h2>

	<#if command.pointSet.points?size == 0>
		<p><em>No points exist for the selected route and year of study</em></p>
	<#else>
		<div class="monitoring-points">
        	<#macro pointsInATerm term>
        		<div class="striped-section">
        			<h2 class="section-title">${term}</h2>
        			<div class="striped-section-contents">
        				<#list command.monitoringPointsByTerm[term]?sort_by("week") as point>
        					<div class="item-info row-fluid point">
        						<div class="span12">
        							<div class="pull-right">
        								<a class="btn btn-primary" href="<@url page="/${command.dept.code}/${point.id}/record?returnTo=${(info.requestedUri!'')?url}"/>">
        									Record
        								</a>
        							</div>
        							${point.name} (<@fmt.weekRanges point />)
        						</div>
        					</div>
        				</#list>
        			</div>
        		</div>
        	</#macro>
			<#list ["Autumn", "Christmas vacation", "Spring", "Easter vacation", "Summer", "Summer vacation"] as term>
				<#if command.monitoringPointsByTerm[term]??>
					<@pointsInATerm term/>
				</#if>
			</#list>
        </div>
	</#if>
</#if>

<script type="text/javascript" src="/static/libs/jquery-tablesorter/jquery.tablesorter.min.js"></script>
<script>
	var setsByRouteByAcademicYear = {
		<#list command.setsByRouteByAcademicYear?keys as academicYear>
			"${academicYear}" : [
				<#list command.setsByRouteByAcademicYear[academicYear]?keys?sort_by("code") as route>
					{
						"code" : "${route.code}",
						"name" : "${route.name}",
						"sets" : [
							<#list command.setsByRouteCodeByAcademicYear(academicYear, route) as set>
								{
									"id" : "${set.id}",
									"year" : "<#if set.year??>${set.year}<#else>All</#if>"
								}
								<#if set_has_next>,</#if>
							</#list>
						]
					}
					<#if route_has_next>,</#if>
				</#list>
			]
			<#if academicYear_has_next>,</#if>
		</#list>
	};

	jQuery(function($){
		$('#missed-monitoring-points')
			.sortableTable()
			.trigger('sorton', [[[$('#missed-monitoring-points th').length - 1,1]]]);
	});
</script>

</#escape>