<#macro academicYearSwitcher path academicYear thisAcademicYear>
	<form class="form-inline" action="${path}">
		<label>Academic year
			<select name="academicYear" class="input-small">
				<#assign academicYears = [thisAcademicYear.previous.toString, thisAcademicYear.toString, thisAcademicYear.next.toString] />
				<#list academicYears as year>
					<option <#if academicYear.toString == year>selected</#if> value="${year}">${year}</option>
				</#list>
			</select>
		</label>
		<button type="submit" class="btn btn-primary">Change</button>
	</form>
</#macro>

<#macro attendanceIcon pointMap point>
	<#local checkpointData = mapGet(pointMap, point) />

	<#local title>${point.name} (<@fmt.monitoringPointFormat point true />)</#local>

	<#if checkpointData.state == "attended">
		<#local class = "icon-ok attended" />
		<#local title = "Attended: " + title />
	<#elseif checkpointData.state == "authorised">
		<#local class = "icon-remove-circle authorised" />
		<#local title = "Missed (authorised): " + title />
	<#elseif checkpointData.state == "unauthorised">
		<#local class = "icon-remove unauthorised" />
		<#local title = "Missed (unauthorised): " + title />
	<#elseif checkpointData.state == "late">
		<#local class = "icon-warning-sign late" />
		<#local title = "Unrecorded: " + title />
	<#else>
		<#local class = "icon-minus" />
	</#if>

	<#local titles = [title] />

	<#if checkpointData.recorded?has_content>
		<#local titles = titles + [checkpointData.recorded] />
	</#if>

	<#if checkpointData.hasNote>
		<#local titles = titles + [checkpointData.note] />
	</#if>

	<#local renderedTitle>
		<#list titles as t>
			<#if (titles?size > 1)>
				<p>${t}</p>
			<#else>
				${t}
			</#if>
		</#list>
	</#local>
	<i class="use-tooltip icon-fixed-width ${class}" title="${renderedTitle}" data-container="body" data-html="true"></i>
</#macro>

<#macro attendanceLabel pointMap point>
	<#assign checkpointData = mapGet(pointMap, point) />
	<#if checkpointData.state == "attended">
		<span
			class="label label-success use-tooltip"
			title="<p>Attended</p><p>${checkpointData.recorded}</p>"
			data-container="body"
			data-html="true"
		>
			Attended
		</span>
	<#elseif checkpointData.state == "authorised">
		<span
			class="label label-info use-tooltip"
			title="<p>Missed (authorised)</p><p>${checkpointData.recorded}</p>"
			data-container="body"
			data-html="true"
		>
			Missed
		</span>
	<#elseif checkpointData.state == "unauthorised">
		<span
			class="label label-important use-tooltip"
			title="<p>Missed (unauthorised)</p><p>${checkpointData.recorded}</p>"
			data-container="body"
			data-html="true"
		>
			Missed
		</span>
	<#elseif checkpointData.state == "late">
		<span class="label label-warning use-tooltip" title="Unrecorded" data-container="body">Unrecorded</span>
	</#if>
</#macro>

<#macro attendanceButtons>
	<div style="display:none;" class="forCloning">
		<div class="btn-group" data-toggle="buttons-radio">
			<button
					type="button"
					class="btn use-tooltip"
					data-state=""
					title="Set to 'Not recorded'"
					data-html="true"
					data-container="body"
					>
				<i class="icon-minus icon-fixed-width"></i>
			</button>
			<button
					type="button"
					class="btn btn-unauthorised use-tooltip"
					data-state="unauthorised"
					title="Set to 'Missed (unauthorised)'"
					data-html="true"
					data-container="body"
					>
				<i class="icon-remove icon-fixed-width"></i>
			</button>
			<button
					type="button"
					class="btn btn-authorised use-tooltip"
					data-state="authorised"
					title="Set to 'Missed (authorised)'"
					data-html="true"
					data-container="body"
					>
				<i class="icon-remove-circle icon-fixed-width"></i>
			</button>
			<button
					type="button"
					class="btn btn-attended use-tooltip"
					data-state="attended"
					title="Set to 'Attended'"
					data-html="true"
					data-container="body"
					>
				<i class="icon-ok icon-fixed-width"></i>
			</button>
		</div>
	</div>
</#macro>

<#macro groupedPointsInATerm pointsMap term department permission_button_function>
	<div class="striped-section">
		<h2 class="section-title">${term}</h2>
		<div class="striped-section-contents">
			<#list pointsMap[term] as groupedPoint>
				<div class="item-info row-fluid point">
					<div class="span12">
						<div class="pull-right">
							${permission_button_function(groupedPoint)}
						</div>
					${groupedPoint.name}
						(<a class="use-tooltip" data-html="true" title="<@fmt.wholeWeekDateFormat groupedPoint.validFromWeek groupedPoint.requiredFromWeek command.academicYear />">
						<@fmt.monitoringPointWeeksFormat groupedPoint.validFromWeek groupedPoint.requiredFromWeek command.academicYear department />
					</a>
						):
						<#if command.allRoutes?? && groupedPoint.routes?size == command.allRoutes?size>
							All routes
						<#else>
							<#local popoverContent>
								<ul class="unstyled">
									<#if command.allRoutes??>
										<#list command.allRoutes as route>
											<#local isInPoint = false />
											<#list groupedPoint.routes as pointRoutePair>
												<#if pointRoutePair._1().code == route.code>
													<li>
														<@fmt.route_name route />

													</li>
												</#if>
											</#list>
										</#list>
									</#if>
									<#list groupedPoint.routes as pointRoutePair>
										<#if !pointRoutePair._2()><li><span title="${pointRoutePair._1().department.name}"><@fmt.route_name pointRoutePair._1() /></span></li></#if>
									</#list>
								</ul>
							</#local>
							<a class="use-wide-popover" data-content="${popoverContent?html}" data-html="true" data-placement="bottom">
								<@fmt.p groupedPoint.routes?size "route" />
							</a>
						</#if>
					</div>
				</div>
			</#list>
		</div>
	</div>
</#macro>