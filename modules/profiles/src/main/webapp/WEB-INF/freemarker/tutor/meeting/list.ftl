<#escape x as x?html>
	<#assign can_read_meetings = can.do("Profiles.MeetingRecord.Read", profile) />
	<#assign can_create_meetings = can.do("Profiles.MeetingRecord.Create", profile) />

	<section class="meetings">
		<#if can_read_meetings>
			<h5>Record of meetings</h5>
		</#if>

		<#if can_create_meetings>
			<a class="btn-like new" href="<@routes.meeting_record studentCourseDetails.scjCode?replace("/","_") />" title="Create a new record"><i class="icon-edit"></i> New record</a>
			<#if isSelf!false>
				<small class="use-tooltip muted" data-placement="bottom" title="Meeting records are currently visible only to you and your personal tutor.">Who can see this information?</small>
			<#else>
				<small class="use-tooltip muted" data-placement="bottom" title="Meeting records are currently visible only to the student and their personal tutor.">Who can see this information?</small>
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
									<a href="<@routes.edit_meeting_record meeting />" class="btn-like edit-meeting-record" title="Edit record"><i class="icon-edit" ></i></a>
									<a href="<@routes.delete_meeting_record meeting />" class="btn-like delete-meeting-record" title="Delete record"><i class="icon-trash"></i></a>
									<a href="<@routes.restore_meeting_record meeting />" class="btn-like restore-meeting-record" title="Restore record"><i class="icon-repeat"></i></a>
									<a href="<@routes.purge_meeting_record meeting />" class="btn-like purge-meeting-record" title="Purge record"><i class="icon-remove"></i></a>
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
								<@fmt.download_attachments meeting.attachments "/tutor/meeting/${meeting.id}/" "for this meeting record" "${meeting.title?url}" />
							</#if>
							<#include "_meeting_record_state.ftl" />
						</div>
					</details>
				</#list>
			</#if>
		</#if>
	</section>
</#escape>

