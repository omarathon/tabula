<#escape x as x?html>
	<#if !command.point.pointType?? || command.point.pointType.dbValue != "meeting">
		<div class="alert alert-error">
			Specified monitoring point is not a Meeting point
		</div>
	<#else>

		<p>
			Monitoring point requires ${command.point.meetingQuantity}
			<#list command.point.meetingFormats as format>
				${format.description}<#if format_has_next> or </#if>
			</#list>
			with the student's
			<#list command.point.meetingRelationships as relationship>
			${relationship.description}<#if relationship_has_next> or </#if>
			</#list>
			on or after <@fmt.singleWeekFormat command.point.validFromWeek command.point.pointSet.academicYear command.point.pointSet.route.department />
		</p>

		<#if meetingsStatuses?size == 0>
			<p><em>There were no meetings found.</em></p>
		<#else>

			<table class="table table-bordered table-condensed table-hover">
				<thead>
					<tr>
						<th>Meeting</th>
						<th>Status</th>
					</tr>
				</thead>
				<tbody>
					<#list meetingsStatuses as meetingStatus>
						<#assign meeting = meetingStatus._1() />
						<#assign reasons = meetingStatus._2() />
						<tr>
							<td>
								${(meeting.format.description)!"Unknown format"} with
								${meeting.relationship.relationshipType.agentRole}
								created <@fmt.date meeting.lastUpdatedDate />
							</td>
							<td>
								<#if reasons?size == 0>
									<i class="icon-fixed-width icon-ok"></i>
								<#else>
									<a class="use-popover" id="popover-meeting-status-${meetingStatus_index}" data-html="true" data-content="<#list reasons as reason>${reason}<#if reason_has_next><br /></#if></#list>">
										<i class="icon-fixed-width icon-remove"></i>
									</a>
								</#if>
							</td>
						</tr>
					</#list>
				</tbody>
			</table>

		</#if>
	</#if>
</#escape>