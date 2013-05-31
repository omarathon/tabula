<#escape x as x?html>
<section id="personal-development" class="clearfix">
	<#if profile.personalTutors??>
		<h4>Personal tutor<#if profile.personalTutors?size gt 1>s</#if></h4>

		<#if profile.personalTutors?size == 0>
			<p>
				Not recorded
				<#if can.do("Profiles.PersonalTutor.Update", profile) && (profile.studyDetails.studyDepartment)?? && profile.studyDetails.studyDepartment.canEditPersonalTutors >
					<a id="edit-tutor-link" href="<@routes.tutor_edit_no_tutor student=profile.universityId />"><i class="icon-edit"></i></a>
				</#if>
			</p>
		</#if>

		<div class="tutors clearfix row">
		<#list profile.personalTutors as relationship>
			<#assign personalTutor = relationship.agentMember />
			<div class="tutor clearfix span4">
				<#if !personalTutor??>
					${relationship.agentName} <span class="muted">External to Warwick</span>
					<#if can.do("Profiles.PersonalTutor.Update", profile) && (profile.studyDetails.studyDepartment)?? && profile.studyDetails.studyDepartment.canEditPersonalTutors >
						<a id="edit-tutor-link" href="<@routes.tutor_edit_no_tutor student=profile.universityId />"><i class="icon-edit"></i></a>
					</#if>
				<#else>
					<div class="photo">
						<img src="<@routes.relationshipPhoto profile relationship />" />
					</div>
					<h5>
						${personalTutor.fullName!"Personal tutor"}
						<#if can.do("Profiles.PersonalTutor.Update", profile) && (profile.studyDetails.studyDepartment)?? && profile.studyDetails.studyDepartment.canEditPersonalTutors >
							<a id="edit-tutor-link" href="<@routes.tutor_edit student=profile.universityId currentTutor=personalTutor/>"><i class="icon-edit"></i></a>
						</#if>
					</h5>
					<#if personalTutor.universityId == viewer.universityId>
						<span class="muted">(you)</span>
					<#else>
						<#if personalTutor.email??>
							<p><i class="icon-envelope"></i> <a href="mailto:${personalTutor.email}">${personalTutor.email}</a></p>
						</#if>
					</#if>
				</#if>
			</div>
		</#list>
		</div>

		<#if profile.hasAPersonalTutor>
			<#include "../tutor/meeting/list.ftl" />
		</#if>

		<div id="modal" class="modal hide fade" style="display:none;">
			<div class="modal-header"></div>
			<div class="modal-body"></div>
			<div class="modal-footer"></div>
		</div>
	</#if>
</section>
</#escape>