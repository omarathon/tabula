<#escape x as x?html>
<section id="personal-development" class="clearfix">

	<#if RequestParameters.action??>
		<#if RequestParameters.action?? && RequestParameters.action == "tutorremoved" || RequestParameters.action == "tutorchanged">
			<div id="tutorsMessage" class="alert alert-success">
				<button type="button" class="close" data-dismiss="alert">&times;</button>
				<p>
					<#if RequestParameters.action = "tutorremoved">
						<strong>${tutor.fullName}</strong>  is no longer ${profile.firstName}'s personal tutor.
					<#else>
						<strong>${tutor.fullName}</strong> is now ${profile.firstName}'s personal tutor.
					</#if>
				</p>
			</div>
		</#if>
	</#if>

	<#if profile.personalTutors??>
		<h4>Personal tutor<#if profile.personalTutors?size gt 1>s</#if></h4>

		<#if profile.personalTutors?size != 0>
			<a class="add-tutor-link" href="<@routes.tutor_edit_no_tutor student=profile.universityId />" data-target="#modal-change-tutor"><i class="icon-plus"></i> Add another tutor</a>
		</#if>

		<#if profile.personalTutors?size == 0>
			<p>
				Not recorded
				<#if can.do("Profiles.PersonalTutor.Update", profile) && (profile.studyDetails.studyDepartment)?? && profile.studyDetails.studyDepartment.canEditPersonalTutors >
					<a class="edit-tutor-link" href="<@routes.tutor_edit_no_tutor student=profile.universityId />" data-target="#modal-change-tutor"><i class="icon-edit"></i></a>
					
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
						<a class="edit-tutor-link" href="<@routes.tutor_edit_no_tutor student=profile.universityId />"  data-target="#modal-change-tutor"><i class="icon-edit"></i></a>
					</#if>
				<#else>
					<div class="photo">
						<img src="<@routes.relationshipPhoto profile relationship />" />
					</div>
					<h5>
						${personalTutor.fullName!"Personal tutor"}
						<#if can.do("Profiles.PersonalTutor.Update", profile) && (profile.studyDetails.studyDepartment)?? && profile.studyDetails.studyDepartment.canEditPersonalTutors >
							<a class="edit-tutor-link" href="<@routes.tutor_edit student=profile.universityId currentTutor=personalTutor/>" data-target="#modal-change-tutor"><i class="icon-edit"></i></a>
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
	
		<div id="modal" class="modal hide fade" style="display:none;"></div>

		<div id="modal-change-tutor" class="modal hide fade"></div>

		<script type="text/javascript">
		jQuery(function($){
			// load edit personal tutor
			$("#personal-development").on("click", ".edit-tutor-link, .add-tutor-link", function(e) {
				e.preventDefault();
				var url = $(this).attr('href');
				$("#modal-change-tutor").load(url,function(){
					$("#modal-change-tutor").modal('show');
				});
			});
		});
		</script>

	</#if>
</section>
</#escape>