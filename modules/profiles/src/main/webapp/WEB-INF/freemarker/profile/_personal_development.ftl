<section id="personal-development" class="clearfix">
	<h4>Personal tutor</h4>
		<#if profile.personalTutor?is_string>
			<p>
				${profile.personalTutor}
				<#if !profile.personalTutor?string?starts_with("Not ")>
					<span class="muted">External to Warwick</span>
				</#if>
				<#if can.do("Profiles.PersonalTutor.Update", profile)>
					<a id="edit-tutor-link" href="<@routes.tutor_edit_no_tutor student=profile.universityId />"><i class="icon-edit"></i></a>
				</#if>
			</p>
		<#else>
			<div class="tutor clearfix">
				<div class="photo">
					<img src="<@routes.tutorPhoto profile />" />
				</div>
				<h5>
					${profile.personalTutor.fullName}
					<#if can.do("Profiles.PersonalTutor.Update", profile)>
						<a id="edit-tutor-link" href="<@routes.tutor_edit student=profile.universityId tutor=profile.personalTutor/>"><i class="icon-edit"></i></a>
					</#if>
				</h5>
				<#if profile.personalTutor.universityId == viewer.universityId>
					<span class="muted">(you)</span>
				<#else>
					<#if profile.personalTutor.email??>
						<p><i class="icon-envelope"></i> <a href="mailto:${profile.personalTutor.email}">${profile.personalTutor.email}</a></p>
					</#if>
				</#if>
			</div>
		</#if>
		
		<#if profile.hasAPersonalTutor>
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
							<details>
								<summary><span class="date"><@fmt.date meeting.meetingDate /></span> ${meeting.title}</summary>
								
								<#if meeting.description??>
									<div class="description">${meeting.description}</div>
								</#if>
								
								<small class="muted">Published by ${meeting.creator.fullName}, <@fmt.date meeting.lastUpdatedDate /></small>
							</details>
						</#list>
					</#if>
				</#if>
			</section>
		</#if>
</section>

