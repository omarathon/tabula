<#escape x as x?html>
	<section class="meetings">
		<#if can.do("Profiles.MeetingRecord.Read", profile)>
			<h5>Record of meetings</h5>
		</#if>

		<#if can.do("Profiles.MeetingRecord.Create", profile)>
			<a class="new" href="<@routes.meeting_record profile.universityId />" title="Create a new record"><i class="icon-edit"></i> New record</a>
			<#if isSelf!false>
				<small class="use-tooltip muted" data-placement="bottom" title="Meeting records are currently visible only to you and your personal tutor.">Who can see this information?</small>
			<#else>
				<small class="use-tooltip muted" data-placement="bottom" title="Meeting records are currently visible only to the student and their personal tutor.">Who can see this information?</small>
			</#if>
		</#if>
		<#if can.do("Profiles.MeetingRecord.Read", profile)>
			<a class="toggle-all-details open-all-details" title="Expand all meetings"><i class="icon-plus"></i> Expand all</a>
			<a class="toggle-all-details close-all-details hide" title="Collapse all meetings"><i class="icon-minus"></i> Collapse all</a>
		</#if>

		<#if can.do("Profiles.MeetingRecord.Read", profile)>
			<#if meetings??>
				<#list meetings as meeting>
					<#assign deletedClasses><#if meeting.deleted>deleted muted</#if></#assign>

					<#if openMeeting?? && openMeeting.id == meeting.id>
						<#assign openClass>open</#assign>
						<#assign openAttribute>open="open"</#assign>
					<#else>
						<#assign openClass></#assign>
						<#assign openAttribute></#assign>
					</#if>

					<details class="meeting ${deletedClasses} ${openClass!}" ${openAttribute!}>
						<summary><span class="date"><@fmt.date date=meeting.meetingDate includeTime=false /></span> ${meeting.title}
							<#if !meeting.approved && viewer.universityId == meeting.creator.universityId>
								<a href="<@routes.delete_meeting_record meeting.id />" class="delete-meeting-record" title="Delete record">
									<i class="meeting-record-toolbar icon-trash"></i>
								</a>
								<a href="<@routes.restore_meeting_record meeting.id />" class="restore-meeting-record" title="Restore record">
									<i class="meeting-record-toolbar icon-repeat"></i>
								</a>
								<a href="<@routes.purge_meeting_record meeting.id />" class="purge-meeting-record" title="Purge record">
									<i class="meeting-record-toolbar icon-remove"></i>
								</a>
							</#if>
						</summary>

						<#if meeting.description??>
							<div class="description"><#noescape>${meeting.description}</#noescape></div>
						</#if>

						<#if meeting.attachments?size gt 0>
							<@fmt.download_attachments meeting.attachments "/tutor/meeting/${meeting.id}/" "for this meeting record" "${meeting.title?url}" />
						</#if>

						<small class="muted">${(meeting.format.description)!"Unknown format"}. Published by ${meeting.creator.fullName}, <@fmt.date meeting.lastUpdatedDate /></small>
					</details>
				</#list>
			</#if>
		</#if>
	</section>
</#escape>

