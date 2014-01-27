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

	<#if checkpointData.note??>
		<#local note>
			${checkpointData.note.truncatedNote}
			<#if (checkpointData.note.truncatedNote?length > 0)>
				<br/>
			</#if>
			<a class='attendance-note-modal' href='<@routes.viewNote checkpointData.note.student checkpointData.note.point />'>View attendance note</a>
		</#local>
		<#local titles = titles + [note] />
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
	<i class="use-popover icon-fixed-width ${class}" data-content="${renderedTitle}" data-html="true"></i>
</#macro>

<#macro attendanceLabel pointMap point>
	<#local checkpointData = mapGet(pointMap, point) />

	<#local title>${point.name} (<@fmt.monitoringPointFormat point true />)</#local>

	<#if checkpointData.state == "attended">
		<#local class = "label-success" />
		<#local title = "Attended: " + title />
		<#local label = "Attended" />
	<#elseif checkpointData.state == "authorised">
		<#local class = "label-info" />
		<#local title = "Missed (authorised): " + title />
		<#local label = "Missed (authorised)" />
	<#elseif checkpointData.state == "unauthorised">
		<#local class = "label-important" />
		<#local title = "Missed (unauthorised): " + title />
		<#local label = "Missed (unauthorised)" />
	<#elseif checkpointData.state == "late">
		<#local class = "label-warning" />
		<#local title = "Unrecorded: " + title />
		<#local label = "Unrecorded" />
	</#if>

	<#local titles = [title] />

	<#if checkpointData.recorded?has_content>
		<#local titles = titles + [checkpointData.recorded] />
	</#if>

	<#if checkpointData.note??>
		<#local note>
			${checkpointData.note.truncatedNote}
			<br/>
			<a class='attendance-note-modal' href='<@routes.viewNote checkpointData.note.student checkpointData.note.point />'>View attendance note</a>
		</#local>
		<#local titles = titles + [note] />
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

	<#if (checkpointData.state?length > 0)>
		<span class="use-popover label ${class}" data-content="${renderedTitle}" data-html="true" data-placement="left">${label}</span>
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