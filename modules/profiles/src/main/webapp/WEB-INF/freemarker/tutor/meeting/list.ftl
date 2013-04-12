<#escape x as x?html>
	<section class="meetings">
		<#if can.do("Profiles.MeetingRecord.Read", profile)>
			<h5>Record of meetings</h5>
		</#if>

		<#if can.do("Profiles.MeetingRecord.Create", profile)>
			<a class="new" href="<@routes.meeting_record profile.universityId />" title="Create a new record"><i class="icon-edit"></i> New record</a>
			<small class="muted">Meeting records are currently visible to the student, tutor, and departmental administrators</small>
		</#if>
		<#if can.do("Profiles.MeetingRecord.Read", profile)>
			<a class="toggle-all-details open-all-details" title="Expand all meetings"><i class="icon-plus"></i> Expand all</a>
			<a class="toggle-all-details close-all-details hide" title="Collapse all meetings"><i class="icon-minus"></i> Collapse all</a>
		</#if>

		<#if can.do("Profiles.MeetingRecord.Read", profile)>
			<#if meetings??>
				<#list meetings as meeting>
					<details<#if openMeeting?? && openMeeting.id == meeting.id> open="open" class="open"</#if>>
						<summary><span class="date"><@fmt.date date=meeting.meetingDate includeTime=false /></span> ${meeting.title}</summary>

						<#if meeting.description??>
							<div class="description"><#noescape>${meeting.description}</#noescape></div>
						</#if>

						<#if meeting.attachments?size gt 0>
							Files to download!
						<a class="long-running use-tooltip form-post" 
							 href="<@url page='/admin/module/${module.code}/assignments/${assignment.id}/submissions.zip'/>" 
							 title="Download the files for this meeting record." 
							 data-container="body"><i class="icon-download"></i> Download files
						</a>							
						<#else>
							No files.
						</#if>

						<small class="muted">Published by ${meeting.creator.fullName}, <@fmt.date meeting.lastUpdatedDate /></small>
					</details>
				</#list>
			</#if>
		</#if>
	</section>
</#escape>