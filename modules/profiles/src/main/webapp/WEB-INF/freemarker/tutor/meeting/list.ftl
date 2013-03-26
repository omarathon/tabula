<#escape x as x?html>
	<section class="meetings">
		<#if can.do("Profiles.MeetingRecord.Read", profile)>
			<h5>Record of meetings</h5>
		</#if>
		
		<#if can.do("Profiles.MeetingRecord.Create", profile)>
			<a class="new" href="<@routes.meeting_record profile.universityId />" title="Create a new record"><i class="icon-edit"></i> New record</a>
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
						
						<small class="muted">Published by ${meeting.creator.fullName}, <@fmt.date meeting.lastUpdatedDate /></small>
					</details>
				</#list>
			</#if>
		</#if>
	</section>
</#escape>