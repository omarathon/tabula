<#import "attendance_variables.ftl" as attendance_variables />
<#import "/WEB-INF/freemarker/_profile_link.ftl" as pl />

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

<#function sortClass field command>
	<#list command.sortOrder as order>
		<#if order.propertyName == field>
			<#if order.ascending>
				<#return "headerSortDown" />
			<#else>
				<#return "headerSortUp" />
			</#if>
		</#if>
	</#list>
	<#return "" />
</#function>

<#macro pagination currentPage totalResults resultsPerPage extra_classes="">
	<#local totalPages = (totalResults / resultsPerPage)?ceiling />
	<div class="pagination pagination-right ${extra_classes}">
		<ul>
			<#if currentPage lte 1>
				<li class="disabled"><span>&laquo;</span></li>
			<#else>
				<li><a href="?page=${currentPage - 1}" data-page="${currentPage - 1}">&laquo;</a></li>
			</#if>

			<#list 1..totalPages as page>
				<#if page == currentPage>
					<li class="active"><span>${page}</span></li>
				<#else>
					<li><a href="?page=${page}" data-page="${page}">${page}</a></li>
				</#if>
			</#list>

			<#if currentPage gte totalPages>
				<li class="disabled"><span>&raquo;</span></li>
			<#else>
				<li><a href="?page=${currentPage + 1}" data-page="${currentPage + 1}">&raquo;</a></li>
			</#if>
		</ul>
	</div>
</#macro>

<#macro manageStudentTable
	membershipItems
	doSorting=false
	command=""
	checkboxName=""
	onlyShowCheckboxForStatic=false
	checkAll=false
	showRemoveButton=false
	showResetButton=false
>

	<#if (membershipItems?size > 0)>

		<table class="manage-student-table table table-bordered table-striped table-condensed table-hover table-sortable table-checkable sticky-table-headers tabula-darkRed tablesorter sb-no-wrapper-table-popout">
			<thead>
			<tr>
				<th class="profile_link-col"></th>
				<th style="width: 50px;" <#if doSorting> class="${sortClass("source", command)} sortable" data-field="source"</#if>>Source</th>
				<th <#if doSorting> class="${sortClass("firstName", command)} sortable" data-field="firstName"</#if>>First name</th>
				<th <#if doSorting> class="${sortClass("lastName", command)} sortable" data-field="lastName"</#if>>Last name</th>
				<th <#if doSorting> class="${sortClass("universityId", command)} sortable" data-field="universityId"</#if>>ID</th>
				<th <#if doSorting> class="${sortClass("userId", command)} sortable" data-field="userId"</#if>>User</th>
				<th>Schemes</th>
				<#if checkboxName?has_content>
					<th style="width: 65px; padding-right: 5px;" <#if checkAll>class="for-check-all"</#if>>
						<#if showRemoveButton>
							<input class="btn btn-warning btn-small use-tooltip"
							  <#if findCommandResult.membershipItems?size == 0>disabled</#if>
							  type="submit"
							  name="${ManageSchemeMappingParameters.manuallyExclude}"
							  value="Remove"
							  title="Remove selected students from this scheme"
							  style="margin-left: 0.5em;"
							/>
						</#if>
						<#if (showResetButton && (editMembershipCommandResult.includedStudentIds?size > 0 || editMembershipCommandResult.excludedStudentIds?size > 0))>
							<input class="btn btn-warning btn-small use-tooltip"
								   type="submit"
								   style="float: right; padding-left: 5px; padding-right: 5px; margin-left: 5px;"
								   name="${ManageSchemeMappingParameters.resetMembership}"
								   value="Reset"
								   data-container="body"
								   title="Restore the manually removed and remove the manually added students selected"
							/>
						</#if>
					</th>
				</#if>
			</tr>
			</thead>
			<tbody>
				<#list membershipItems as item>
					<tr class="${item.itemTypeString}">
						<td class="profile_link"><@pl.profile_link item.universityId /></td>
						<td>
							<#if item.itemTypeString == "static">
								<span class="use-tooltip" title="Automatically linked from SITS" data-placement="right"><i class="icon-list-alt"></i></span>
							<#elseif item.itemTypeString == "exclude">
								<span class="use-tooltip" title="Removed manually, overriding SITS" data-placement="right"><i class="icon-ban-circle"></i></span>
							<#else>
								<span class="use-tooltip" title="Added manually" data-placement="right"><i class="icon-hand-up"></i></span>
							</#if>
						</td>
						<td>${item.firstName}</td>
						<td>${item.lastName}</td>
						<td><a class="profile-link" href="<@routes.attendance.profile item />">${item.universityId}</a></td>
						<td>${item.userId}</td>
						<td>
							<#if item.existingSchemes?size == 0>
								0 schemes
							<#else>
								<#local popovercontent>
									<ul>
										<#list item.existingSchemes as scheme>
											<li>${scheme.displayName}</li>
										</#list>
									<ul>
								</#local>

								<span
									class="use-tooltip"
									data-container="body"
									title="See which schemes apply to this student"
								>
									<span
										class="use-popover"
										data-container="body"
										data-html="true"
										data-content="${popovercontent}"
										data-placement="top"
										>
										<@fmt.p item.existingSchemes?size "scheme" />
									</span>
								</span>
							</#if>
						</td>
						<#if checkboxName?has_content>
							<td>
								<#if !onlyShowCheckboxForStatic || item.itemTypeString == "static">
									<input type="checkbox" name="${checkboxName}" value="${item.universityId}" />
								</#if>
							</td>
						</#if>
					</tr>
				</#list>
			</tbody>
		</table>

		<div id="profile-modal" class="modal fade profile-subset"></div>
	</#if>
</#macro>

<#macro groupedPointsBySection pointsMap sectionName>
<div class="striped-section">
	<h2 class="section-title">${sectionName}</h2>
	<div class="striped-section-contents">
		<#list pointsMap[sectionName] as groupedPoint>
			<div class="item-info row-fluid point">
				<#nested groupedPoint />
			</div>
		</#list>
	</div>
</div>
</#macro>

<#macro groupedPointSchemePopover groupedPoint>
	<#local popoverContent>
		<ul>
			<#list groupedPoint.schemes?sort_by("displayName") as scheme>
				<li>${scheme.displayName}</li>
			</#list>
		</ul>
	</#local>
	<a href="#" class="use-popover" data-content="${popoverContent}" data-html="true" data-placement="right">
		<@fmt.p groupedPoint.schemes?size "scheme" />
	</a>
</#macro>

<#function formatResult department checkpoint="" point="" student="" note="">
	<#if checkpoint?has_content>
		<#if note?has_content>
			<#return attendanceMonitoringCheckpointFormatter(department, checkpoint, note) />
		<#else>
			<#return attendanceMonitoringCheckpointFormatter(department, checkpoint) />
		</#if>
	<#else>
		<#if note?has_content>
			<#return attendanceMonitoringCheckpointFormatter(department, point, student, note) />
		<#else>
			<#return attendanceMonitoringCheckpointFormatter(department, point, student) />
		</#if>
	</#if>
</#function>

<#macro checkpointDescription department checkpoint="" point="" student="" note="">
	<#local formatResult = formatResult(department, checkpoint, point, student, note) />
	<#if formatResult.metadata?has_content><p>${formatResult.metadata}</p></#if>
</#macro>

<#macro checkpointLabel department checkpoint="" point="" student="" note="">
	<#local formatResult = formatResult(department, checkpoint, point, student, note) />
	<#local popoverContent>
		<#if formatResult.status?has_content><p>${formatResult.status}</p></#if>
		<#if formatResult.metadata?has_content><p>${formatResult.metadata}</p></#if>
		<#if formatResult.noteType?has_content><p>${formatResult.noteType}</p></#if>
		<#if formatResult.noteText?has_content><p>${formatResult.noteText}</p></#if>
		<#if formatResult.noteUrl?has_content><p><a class='attendance-note-modal' href='${formatResult.noteUrl}'>View attendance note</a></p></#if>
	</#local>
	<span class="use-popover label ${formatResult.labelClass}" data-content="${popoverContent}" data-html="true" data-placement="left">${formatResult.labelText}</span>
	<span class="hidden-desktop visible-print">
		<#if formatResult.metadata?has_content>${formatResult.metadata}<br /></#if>
		<#if formatResult.noteType?has_content>${formatResult.noteType}<br /></#if>
		<#if formatResult.noteText?has_content>${formatResult.noteText}</#if>
	</span>
</#macro>

<#macro checkpointSelect department id name checkpoint="" point="" student="" note="">
	<#local formatResult = formatResult(department, checkpoint, point, student, note) />
	<#local tooltipContent>
		<#if formatResult.metadata?has_content><p>${formatResult.metadata}</p></#if>
		<#if formatResult.noteType?has_content><p>${formatResult.noteType}</p></#if>
		<#if formatResult.noteText?has_content><p>${formatResult.noteText}</p></#if>
	</#local>
	<#local startDateInFuture = point.startDateInFuture />
	<select
		id="${id}"
		name="${name}"
		title="${tooltipContent}"
	>
		<option value="" <#if !checkpoint?has_content >selected</#if>>Not recorded</option>
		<option <#if startDateInFuture>class="disabled" title="This event hasn't happened yet so it can't be marked as missed (unauthorised)" </#if> value="unauthorised" <#if checkpoint?has_content && checkpoint.state.dbValue == "unauthorised">selected</#if>>Missed (unauthorised)</option>
		<option value="authorised" <#if checkpoint?has_content && checkpoint.state.dbValue == "authorised">selected</#if>>Missed (authorised)</option>
		<option <#if startDateInFuture>class="disabled" title="This event hasn't happened yet so it can't be marked as attended" </#if> value="attended" <#if checkpoint?has_content && checkpoint.state.dbValue == "attended">selected</#if>>Attended</option>
	</select>
</#macro>

<#macro checkpointIcon department checkpoint="" point="" student="" note="">
	<#local nonActivePoint = checkpoint?has_content && !checkpoint.activePoint />
	<#local formatResult = formatResult(department, checkpoint, point, student, note) />
	<#local popoverContent>
		<#if nonActivePoint>
			<p>
				This attendance can no longer be edited, because it was recorded for a monitoring scheme (${checkpoint.point.scheme.displayName})
				that no longer applies to this student.
				<a href='http://warwick.ac.uk/tabula/manual/monitoring-points/recording-viewing-points/#nonactivepoints' target='_blank'><i class='icon-question-sign'></i></a>
			</p>
		</#if>
		<#if formatResult.status?has_content><p>${formatResult.status}</p></#if>
		<#if formatResult.metadata?has_content><p>${formatResult.metadata}</p></#if>
		<#if formatResult.noteType?has_content><p>${formatResult.noteType}</p></#if>
		<#if formatResult.noteText?has_content><p>${formatResult.noteText}</p></#if>
		<#if formatResult.noteUrl?has_content><p><a class='attendance-note-modal' href='${formatResult.noteUrl}'>View attendance note</a></p></#if>
	</#local>
	<span class="icon-stack icon-stack-original-size icon-stack-right icon-fixed-width use-popover" data-content="${popoverContent}" data-html="true">
		<i class="icon-fixed-width icon-stack-base ${formatResult.iconClass} <#if nonActivePoint>non-active</#if>" ></i>
		<#if formatResult.noteUrl?has_content><i class="icon-fixed-width icon-stack-small icon-envelope-alt icon-filled-white"></i></#if>
	</span>
</#macro>

<#macro checkpointIconForPointCheckpointPair department student pointCheckpointPair attendanceNotesMap>
	<#if pointCheckpointPair._2()??>
		<#if mapGet(attendanceNotesMap, pointCheckpointPair._1())??>
			<@checkpointIcon
				department=department
				checkpoint=pointCheckpointPair._2()
				note=mapGet(attendanceNotesMap, pointCheckpointPair._1())
			/>
		<#else>
			<@checkpointIcon
				department=department
				checkpoint=pointCheckpointPair._2()
			/>
		</#if>
	<#else>
		<#if mapGet(attendanceNotesMap, pointCheckpointPair._1())??>
			<@checkpointIcon
				department=department
				point=pointCheckpointPair._1()
				student=student
				note=mapGet(attendanceNotesMap, pointCheckpointPair._1())
			/>
		<#else>
			<@checkpointIcon
				department=department
				point=pointCheckpointPair._1()
				student=student
			/>
		</#if>
	</#if>
</#macro>

<#macro listCheckpointIcons department visiblePeriods monthNames result>
	<#list attendance_variables.monitoringPointTermNames as term>
		<#if visiblePeriods?seq_contains(term)>
			<td>
				<#if result.groupedPointCheckpointPairs[term]??>
					<#list result.groupedPointCheckpointPairs[term] as pointCheckpointPair>
						<#if pointCheckpointPair??>
							<@checkpointIconForPointCheckpointPair department result.student pointCheckpointPair result.attendanceNotes />
						<#else>
							<i class="icon-fixed-width"></i>
						</#if>
					</#list>
				<#else>
					<i class="icon-fixed-width"></i>
				</#if>
			</td>
		</#if>
	</#list>
	<#list monthNames as month>
		<#if visiblePeriods?seq_contains(month)>
		<td>
			<#if result.groupedPointCheckpointPairs[month]??>
				<#list result.groupedPointCheckpointPairs[month] as pointCheckpointPair>
					<#if pointCheckpointPair??>
						<@checkpointIconForPointCheckpointPair department result.student pointCheckpointPair result.attendanceNotes />
					<#else>
						<i class="icon-fixed-width"></i>
					</#if>
				</#list>
			<#else>
				<i class="icon-fixed-width"></i>
			</#if>
		</td>
		</#if>
	</#list>
</#macro>

<#macro scrollablePointsTable command department filterResult visiblePeriods monthNames doCommandSorting=true>
	<div class="scrollable-points-table">
		<div class="row">
			<div class="left">
				<table class="students table table-bordered table-striped table-condensed">
					<thead>
					<tr>
						<th class="profile_link-col"></th>
						<th class="student-col <#if doCommandSorting>${sortClass("firstName", command)}</#if> sortable" data-field="firstName">First name</th>
						<th class="student-col <#if doCommandSorting>${sortClass("lastName", command)}</#if> sortable" data-field="lastName">Last name</th>
						<th class="id-col <#if doCommandSorting>${sortClass("universityId", command)}</#if> sortable" data-field="universityId">ID</th>
					</tr>
					</thead>

					<tbody>
						<#list filterResult.results as result>
						<tr class="student">
							<td class="profile_link"><@pl.profile_link result.student.universityId /></td>
							<td class="fname" title="${result.student.firstName}">${result.student.firstName}</td>
							<td class="lname" title="${result.student.lastName}">${result.student.lastName}</td>
							<td class="id"><a class="profile-link" href="<@routes.attendance.profile result.student />">${result.student.universityId}</a></td>
						</tr>
						</#list>
					</tbody>
				</table>
			</div>

			<div class="middle">
				<table class="attendance table tablesorter table-bordered table-striped table-condensed sb-no-wrapper-table-popout">
					<thead>
					<tr>
						<#list attendance_variables.monitoringPointTermNames as term>
							<#if visiblePeriods?seq_contains(term)>
								<th class="${term}-col">${term}</th>
							</#if>
						</#list>
						<#list monthNames as month>
							<#if visiblePeriods?seq_contains(month)>
								<#assign monthMatch = month?matches("([a-zA-Z]{3})[a-zA-Z]*\\s(.*)")[0] />
								<#assign shortMonth>${monthMatch?groups[1]} ${monthMatch?groups[2]}</#assign>
								<th class="${shortMonth}-col">${shortMonth}</th>
							</#if>
						</#list>
						<#if visiblePeriods?size == 0>
							<th>&nbsp;</th>
						</#if>
					</tr>
					</thead>

					<tbody>
						<#list filterResult.results as result>
							<tr class="student">
								<#if visiblePeriods?size == 0 || !result.groupedPointCheckpointPairs?has_content>
									<td colspan="${visiblePeriods?size}"><span class="muted"><em>No monitoring points found</em></span></td>
								<#else>
									<@listCheckpointIcons department visiblePeriods monthNames result />
								</#if>
							</tr>
						</#list>
					</tbody>
				</table>
			</div>

			<div class="right">
				<table class="counts table table-bordered table-striped table-condensed">
					<thead>
					<tr>
						<th class="unrecorded-col <#if doCommandSorting>${sortClass("attendanceCheckpointTotals.unrecorded", command)}</#if> sortable" data-field="attendanceCheckpointTotals.unrecorded">
							<i title="Unrecorded" class="icon-warning-sign icon-fixed-width late"></i>
						</th>
						<th class="missed-col <#if doCommandSorting>${sortClass("attendanceCheckpointTotals.unauthorised", command)}</#if> sortable" data-field="attendanceCheckpointTotals.unauthorised">
							<i title="Missed monitoring points" class="icon-remove icon-fixed-width unauthorised"></i>
						</th>
						<th class="record-col"></th>
					</tr>
					</thead>

					<tbody>
						<#list filterResult.results as result>
						<tr class="student">
							<#if result.groupedPointCheckpointPairs?keys?size == 0>
								<td class="unrecorded">
									<span class="badge badge-success">0</span>
								</td>
								<td class="missed">
									<span class="badge badge-success">0</span>
								</td>
								<td>&nbsp;</td>
							<#else>
								<#nested result />
							</#if>
						</tr>
						</#list>
					</tbody>
				</table>
			</div>
		</div>
	</div>
</#macro>

<#macro checkpointTotalTitle checkpointTotal>
	<#if checkpointTotal.updatedDate.millis == 0>
		(awaiting update)
	<#else>
		Last updated <@fmt.date checkpointTotal.updatedDate />
	</#if>
</#macro>