<#escape x as x?html>

<#macro list studentCourseDetails meetings relationshipType viewerRelationshipTypes="">
	<#local can_read_meetings = can.do_with_selector("Profiles.MeetingRecord.Read", studentCourseDetails, relationshipType) />
	<#local can_create_meetings = can.do_with_selector("Profiles.MeetingRecord.Manage", studentCourseDetails, relationshipType) />
	<#local existingRelationship = ((studentCourseDetails.relationships(relationshipType))![])?size gt 0 />
	<#local is_student = ((viewerUser.universityId)!"")?length gt 0 && viewerUser.universityId == (studentCourseDetails.student.universityId)!"" />

	<#local student_can_schedule_meetings = true />
	<#if existingRelationship>
		<#list studentCourseDetails.relationships(relationshipType) as relationship>
			<#if relationship.agentMember??>
				<#local student_can_schedule_meetings = student_can_schedule_meetings && relationship.agentMember.homeDepartment.studentsCanScheduleMeetings />
			</#if>
		</#list>
	</#if>
	<#local can_create_scheduled_meetings =
		can.do_with_selector("Profiles.ScheduledMeetingRecord.Manage", studentCourseDetails, relationshipType) &&
		(!is_student || student_can_schedule_meetings)
	/>

	<section class="meetings ${relationshipType.id}-meetings" data-target-container="${relationshipType.id}-meetings">
		<div class="list-controls">
			<#if can_read_meetings>
				<h5>Record of meetings</h5>
			</#if>

			<#if existingRelationship && can_create_meetings>
				<a class="btn btn-link new" href="<@routes.profiles.meeting_record studentCourseDetails.urlSafeId relationshipType />" title="Create a new record">New record</a>
			</#if>
			<#if existingRelationship && can_create_scheduled_meetings && features.scheduledMeetings>
				<a class="btn btn-link new" href="<@routes.profiles.create_scheduled_meeting_record studentCourseDetails.urlSafeId relationshipType />" title="Schedule a meeting">Schedule</a>
			</#if>

		</div>
		<#if can_read_meetings>
			<#if meetings??>
				<div class="list-controls">
					<a class="toggle-all-details btn btn-link open-all-details" title="Expand all meetings">Expand all</a>
					<a class="toggle-all-details btn btn-link close-all-details hide" title="Collapse all meetings">Collapse all</a>
				</div>
				<#list meetings as meeting>
					<#local deletedClasses><#if meeting.deleted>deleted subtle</#if></#local>
					<#local pendingAction = meeting.pendingActionBy(viewerUser) />
					<#local pendingActionClasses><#if pendingAction>well</#if></#local>

					<#if (openMeetingId?? && openMeetingId == meeting.id) || pendingAction>
						<#local openClass>open</#local>
						<#local openAttribute>open="open"</#local>
					<#else>
						<#local openClass></#local>
						<#local openAttribute></#local>
					</#if>

					<details class="meeting ${deletedClasses} ${pendingActionClasses} ${openClass!} <#if meeting.scheduled>scheduled<#else>normal</#if>" ${openAttribute!}>
						<summary>
							<span class="date">
								<#if meeting.scheduled || meeting.realTime>
									<@fmt.date date=meeting.meetingDate shortMonth=true includeTime=true />
								<#else>
									<@fmt.date date=meeting.meetingDate shortMonth=true includeTime=false />
								</#if>
							</span>
							<span class="title">${meeting.title!}</span>

							<#if meeting.scheduled>
								<#local can_update_scheduled_meeting = can.do_with_selector("Profiles.ScheduledMeetingRecord.Manage", meeting, relationshipType) />
								<#local editUrl><@routes.profiles.edit_scheduled_meeting_record meeting studentCourseDetails.urlSafeId relationshipType /></#local>
							<#else>
								<#local editUrl><@routes.profiles.edit_meeting_record studentCourseDetails.urlSafeId meeting /></#local>
							</#if>
							<#if ((meeting.scheduled && can_update_scheduled_meeting) || (!meeting.scheduled && viewerUser.universityId! == meeting.creator.universityId && !meeting.approved))>
								<div class="meeting-record-toolbar">
									<a href="${editUrl}" class="btn btn-link edit-meeting-record" title="Edit record">Edit</a>
									<a href="<@routes.profiles.delete_meeting_record meeting />" class="btn btn-link delete-meeting-record" title="Delete record">Delete</a>
									<a href="<@routes.profiles.restore_meeting_record meeting />" class="btn btn-link restore-meeting-record" title="Restore record">Restore</a>
									<a href="<@routes.profiles.purge_meeting_record meeting />" class="btn btn-link purge-meeting-record" title="Purge record">Purge</a>
									<i class="fa fa-spinner fa-spin"></i>
								</div>
							</#if>
						</summary>
						<div class="meeting-body">
							<#if meeting.description??>
								<div class="description">
									<#noescape>${meeting.escapedDescription}</#noescape>
								</div>
							</#if>

							<#if meeting.attachments?? && meeting.attachments?size gt 0>
								<#local mrDownloadUrl><@routes.profiles.download_meeting_record_attachment relationshipType meeting /></#local>
								<@fmt.download_attachments meeting.attachments mrDownloadUrl "for this meeting record" "${meeting.title?url}" />
							</#if>

							<#if meeting.scheduled>
								<@scheduledMeetingState meeting studentCourseDetails/>
							<#else>
								<@meetingState meeting studentCourseDetails/>
							</#if>
						</div>
					</details>
				</#list>
			</#if>
		</#if>
	</section>
</#macro>

<#macro meetingState meeting studentCourseDetails>
	<#if meeting.pendingApproval && !meeting.pendingApprovalBy(viewerUser)>
		<p class="very-subtle">Pending approval. Submitted by ${meeting.creator.fullName}, <@fmt.date meeting.creationDate /></p>
		<div class="alert alert-info">
			This meeting record needs to be approved by <#list meeting.pendingApprovers as approver>${approver.fullName}<#if approver_has_next>, </#if></#list>.
		</div>
	<#elseif meeting.pendingApprovalBy(viewerUser)>
		<p class="very-subtle">Pending approval. Submitted by ${meeting.creator.fullName}, <@fmt.date meeting.creationDate /></p>
		<div class="pending-action alert alert-info">
			This record needs your approval. Please review, then approve or return it with comments.
			<#if meetingApprovalWillCreateCheckpoint[meeting.id]>
				<br />
				Approving this meeting record will mark a monitoring point as attended.
			</#if>
		</div>
		<!-- not a spring form as we don't want the issue of binding multiple sets of data to the same command -->
		<form method="post" class="approval" id="meeting-${meeting.id}" action="<@routes.profiles.save_meeting_approval meeting />" >
			<@form.row>
				<@form.field>
					<label class="radio inline">
						<input type="radio" name="approved" value="true">
						Approve
					</label>
					<label class="radio inline">
						<input class="reject" type="radio" name="approved" value="false">
						Return with comments
					</label>
				</@form.field>
			</@form.row>
			<div class="rejection-comment" style="display:none">
				<@form.row>
					<@form.field>
						<textarea class="big-textarea" name="rejectionComments"></textarea>
					</@form.field>
				</@form.row>
			</div>
			<button type="submit" class="btn btn-primary spinnable spinner-auto">Submit</button>
		</form>
	<#elseif meeting.rejectedBy(viewerMember)>
		<p class="very-subtle">Pending revision. Submitted by ${meeting.creator.fullName}, <@fmt.date meeting.creationDate /></p>
		<div class="alert alert-danger">
			<div class="rejection">
				<p>You sent this record back to the other party, who will review the record and submit it for approval again.</p>
			</div>
		</div>
	<#elseif meeting.pendingRevisionBy(viewerUser)>
		<p class="very-subtle">Pending approval. Submitted by ${meeting.creator.fullName}, <@fmt.date meeting.creationDate /></p>
		<div class="pending-action alert alert-info">
			<#list meeting.rejectedApprovals as rejectedApproval>
				<div class="rejection">
					<p>This record has been returned with comments by ${rejectedApproval.approver.fullName} because:</p>
					<blockquote class="reason">${rejectedApproval.comments}</blockquote>
					<p>Please edit the record and submit it for approval again.</p>
				</div>
			</#list>
		</div>
		<div class="submit-buttons">
			<a class="edit-meeting-record btn btn-primary" href="<@routes.profiles.edit_meeting_record studentCourseDetails.urlSafeId meeting/>">Edit</a>
		</div>
	<#else>
		<p class="very-subtle">
			${(meeting.format.description)!"Unknown format"} between ${(meeting.relationship.agentName)!meeting.relationship.relationshipType.agentRole} and ${(meeting.relationship.studentMember.fullName)!"student"}.
			Created by ${meeting.creator.fullName}, <@fmt.date meeting.lastUpdatedDate />.
			<#if meeting.approved && (meeting.approvedBy?has_content || meeting.approvedDate?has_content)>
				Approved<#if meeting.approvedBy?has_content> by ${meeting.approvedBy.fullName},</#if><#if meeting.approvedDate?has_content> <@fmt.date meeting.approvedDate /></#if>
			</#if>
		</p>
	</#if>
</#macro>

<#macro scheduledMeetingState meeting studentCourseDetails>
	<#local pendingAction = meeting.pendingAction />
	<#local canConvertMeeting = can.do("Profiles.ScheduledMeetingRecord.Confirm", meeting) />

	<#if pendingAction && !canConvertMeeting>
		<p class="very-subtle">Pending confirmation. Submitted by ${meeting.creator.fullName}, <@fmt.date meeting.creationDate /></p>
		<#if !meeting.pendingActionBy(viewerUser)>
			<div class="alert alert-info">
				${meeting.creator.fullName} needs to confirm that this meeting took place.
			</div>
		</#if>
	<#elseif pendingAction && canConvertMeeting>
		<p class="very-subtle">Pending confirmation. ${(meeting.format.description)!"Unknown format"} between ${(meeting.relationship.agentName)!meeting.relationship.relationshipType.agentRole} and ${(meeting.relationship.studentMember.fullName)!"student"}. Created by ${meeting.creator.fullName}, <@fmt.date meeting.lastUpdatedDate /></p>
		<#if meeting.deleted><p class="very-subtle">This meeting has been deleted</p>
		<#else>
			<div class="alert alert-info">
				Please confirm whether this scheduled meeting took place.
			</div>

			<form method="post" class="scheduled-action" id="meeting-${meeting.id}" action="<@routes.profiles.choose_action_scheduled_meeting_record meeting studentCourseDetails.urlSafeId meeting.relationship.relationshipType />" >
				<@form.row>
					<@form.field>
						<label class="radio">
							<input checked type="radio" name="action" value="confirm" data-formhref="<@routes.profiles.confirm_scheduled_meeting_record meeting studentCourseDetails.urlSafeId meeting.relationship.relationshipType />">
							Confirm
						</label>
						<label class="radio">
							<input type="radio" name="action" value="reschedule" />
							Reschedule
						</label>
						<label class="radio">
							<input type="radio" name="action" value="missed" class="reject" data-formhref="<@routes.profiles.missed_scheduled_meeting_record meeting meeting.relationship.relationshipType />">
							Record that the meeting did not take place
						</label>
					</@form.field>
				</@form.row>
				<div class="rejection-comment" style="display:none">
					<@form.row>
						<@form.field>
							<textarea class="big-textarea" name="missedReason"></textarea>
						</@form.field>
					</@form.row>
				</div>
				<div class="ajaxErrors alert alert-danger" style="display: none;"></div>
				<button type="submit" class="btn btn-primary">Submit</button>
			</form>
		</#if>
	<#elseif meeting.missed>
		<div class="alert alert-danger rejection">
			<#if meeting.missedReason?has_content>
				<p> This meeting did not take place because: </p>
				<blockquote class="reason">${meeting.missedReason}</blockquote>
			<#else>
				<p> This meeting did not take place (no reason given) </p>
			</#if>
		</div>
		<p class="very-subtle">${(meeting.format.description)!"Unknown format"} between ${(meeting.relationship.agentName)!meeting.relationship.relationshipType.agentRole} and ${(meeting.relationship.studentMember.fullName)!"student"}. Created by ${meeting.creator.fullName}, <@fmt.date meeting.lastUpdatedDate /></p>
	<#else>
		<p class="very-subtle">${(meeting.format.description)!"Unknown format"} between ${(meeting.relationship.agentName)!meeting.relationship.relationshipType.agentRole} and ${(meeting.relationship.studentMember.fullName)!"student"}. Created by ${meeting.creator.fullName}, <@fmt.date meeting.lastUpdatedDate /></p>
	</#if>
</#macro>

</#escape>
