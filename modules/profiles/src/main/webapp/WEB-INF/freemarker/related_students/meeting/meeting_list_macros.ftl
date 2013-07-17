<#escape x as x?html>

<#macro list studentCourseDetails meetings role>
<#if role=="supervisor">
	<#assign can_read_meetings = can.do("Profiles.Supervisor.MeetingRecord.Read", studentCourseDetails) />
	<#assign can_create_meetings = can.do("Profiles.Supervisor.MeetingRecord.Create", studentCourseDetails) />
	<#assign action_url_prefix = "supervisor" />

<#elseif role="personal tutor">
	<#assign can_read_meetings = can.do("Profiles.PersonalTutor.MeetingRecord.Read", studentCourseDetails) />
	<#assign can_create_meetings = can.do("Profiles.PersonalTutor.MeetingRecord.Create", studentCourseDetails) />
	<#assign action_url_prefix = "tutor" />
</#if>

	<section class="meetings ${action_url_prefix}-meetings" data-target-container="${action_url_prefix}-meetings">

		<#if can_read_meetings>
			<h5>Record of meetings</h5>
		</#if>

		<#if can_create_meetings>
			<a class="btn-like new" href="<@routes.meeting_record studentCourseDetails.scjCode?replace("/","_") action_url_prefix/>" title="Create a new record"><i class="icon-edit"></i> New record</a>
			<#if isSelf!false>
				<small class="use-tooltip muted" data-placement="bottom" title="Meeting records are currently visible only to you and your ${role}(s).">Who can see this information?</small>
			<#else>
				<small class="use-tooltip muted" data-placement="bottom" title="Meeting records are currently visible only to the student and their ${role}(s).">Who can see this information?</small>
			</#if>
		</#if>
		<#if can_read_meetings>
			<a class="toggle-all-details btn-like open-all-details" title="Expand all meetings"><i class="icon-plus"></i> Expand all</a>
			<a class="toggle-all-details btn-like close-all-details hide" title="Collapse all meetings"><i class="icon-minus"></i> Collapse all</a>
		</#if>

		<#if can_read_meetings>
			<#if meetings??>
				<#list meetings as meeting>
					<#assign deletedClasses><#if meeting.deleted>deleted muted</#if></#assign>
					<#assign pendingAction = meeting.pendingActionBy(viewer) />
					<#assign pendingActionClasses><#if pendingAction>well</#if></#assign>

					<#if (openMeeting?? && openMeeting.id == meeting.id) || pendingAction>
						<#assign openClass>open</#assign>
						<#assign openAttribute>open="open"</#assign>
					<#else>
						<#assign openClass></#assign>
						<#assign openAttribute></#assign>
					</#if>

					<details class="meeting ${deletedClasses} ${pendingActionClasses} ${openClass!}" ${openAttribute!}>
						<summary><span class="date"><@fmt.date date=meeting.meetingDate includeTime=false /></span> ${meeting.title!}

							<#if !meeting.approved && viewer.universityId == meeting.creator.universityId>
								<div class="meeting-record-toolbar">
									<a href="<@routes.edit_meeting_record studentCourseDetails.scjCode?replace("/","_") meeting action_url_prefix/>" class="btn-like edit-meeting-record" title="Edit record"><i class="icon-edit" ></i></a>
									<a href="<@routes.delete_meeting_record meeting action_url_prefix/>" class="btn-like delete-meeting-record" title="Delete record"><i class="icon-trash"></i></a>
									<a href="<@routes.restore_meeting_record meeting action_url_prefix/>" class="btn-like restore-meeting-record" title="Restore record"><i class="icon-repeat"></i></a>
									<a href="<@routes.purge_meeting_record meeting action_url_prefix/>" class="btn-like purge-meeting-record" title="Purge record"><i class="icon-remove"></i></a>
									<i class="icon-spinner icon-spin"></i>
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
								<@fmt.download_attachments meeting.attachments "/${action_url_prefix}/meeting/${meeting.id}/" "for this meeting record" "${meeting.title?url}" />
							</#if>
							<@state meeting studentCourseDetails/>
						</div>
					</details>
				</#list>
			</#if>
		</#if>
	</section>
</#macro>

<#macro state meeting studentCourseDetails>
	<#if meeting.pendingApproval && !meeting.pendingApprovalBy(viewer)>
	<small class="muted">Pending approval. Submitted by ${meeting.creator.fullName}, <@fmt.date meeting.creationDate /></small>
	<div class="alert alert-info">
		This meeting record needs to be approved by <#list meeting.pendingApprovers as approver>${approver.fullName}<#if approver_has_next>, </#if></#list>.
	</div>
	<#elseif meeting.pendingApprovalBy(viewer)>
	<small class="muted">Pending approval. Submitted by ${meeting.creator.fullName}, <@fmt.date meeting.creationDate /></small>
	<div class="pending-action alert alert-warning">
		This record needs your approval. Please review, then approve or reject it.
		If you reject it, please explain why.
	</div>
	<!-- not a spring form as we don't want the issue of binding multiple sets of data to the same command -->
	<form method="post" id="meeting-${meeting.id}" action="<@routes.save_meeting_approval meeting action_url_prefix />" >
		<@form.row>
			<@form.field>
				<label class="radio inline">
					<input type="radio" name="approved" value="true">
					Approve
				</label>
				<label class="radio inline">
					<input class="reject" type="radio" name="approved" value="false">
					Reject
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
		<button type="submit" class="btn btn-primary">Submit</button>
	</form>
	<#elseif meeting.rejectedBy(viewer)>
	<small class="muted">Pending revision. Submitted by ${meeting.creator.fullName}, <@fmt.date meeting.creationDate /></small>
	<div class="alert alert-error">
		<div class="rejection">
			<p>You rejected this record. The other party will review the record and submit it for approval again.</p>
		</div>
	</div>
	<#elseif meeting.pendingRevisionBy(viewer)>
	<small class="muted">Pending approval. Submitted by ${meeting.creator.fullName}, <@fmt.date meeting.creationDate /></small>
	<div class="pending-action alert alert-error">
		<#list meeting.rejectedApprovals as rejectedApproval>
			<div class="rejection">
				<p>This record has been rejected by ${rejectedApproval.approver.fullName} because:</p>
				<blockquote class="reason">${rejectedApproval.comments}</blockquote>
				<p>Please edit the record and submit it for approval again.</p>
			</div>
		</#list>
	</div>
	<div class="submit-buttons">
		<a class="edit-meeting-record btn btn-primary" href="<@routes.edit_meeting_record studentCourseDetails.scjCode?replace("/","_") meeting action_url_prefix/>">Edit</a>
	</div>
	<#else>
	<small class="muted">${(meeting.format.description)!"Unknown format"} between ${(meeting.relationship.agentName)!meeting.relationship.relationshipType.description} and ${(meeting.relationship.studentMember.fullName)!"student"}. Created by ${meeting.creator.fullName}, <@fmt.date meeting.lastUpdatedDate /></small>
	</#if>
</#macro>

</#escape>

