<#import "../related_students/meeting/meeting_list_macros.ftl" as meeting_macros />
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

	<#if studentCourseDetails.personalTutors??>
		<h4>Personal tutor<#if studentCourseDetails.personalTutors?size gt 1>s</#if></h4>

		<#assign acceptsPersonalTutorChanges = (studentCourseDetails.sprCode)?? && (studentCourseDetails.department)?? && studentCourseDetails.department.canEditPersonalTutors />

		<#assign acceptsPersonalTutorChanges = (studentCourseDetails.department)?? && studentCourseDetails.department.canEditPersonalTutors />
		<#if studentCourseDetails.hasAPersonalTutor && can.do("Profiles.PersonalTutor.Create", profile) && acceptsPersonalTutorChanges>
			<a class="add-tutor-link" href="<@routes.tutor_edit_no_tutor scjCode=studentCourseDetails.urlSafeId />"
				data-target="#modal-change-tutor"
				data-scj="${studentCourseDetails.scjCode}"
			>
			<i class="icon-plus"></i> Add another tutor
			</a>
		</#if>

		<#if studentCourseDetails.personalTutors?size == 0>
			<p>
				Not recorded
				<#if can.do("Profiles.PersonalTutor.Update", profile) && acceptsPersonalTutorChanges>
					<a class="edit-tutor-link" href="<@routes.tutor_edit_no_tutor scjCode=studentCourseDetails.urlSafeId />"
						data-target="#modal-change-tutor"
						data-scj="${studentCourseDetails.scjCode}"

					>
					<i class="icon-edit"></i>
					</a>

				</#if>
			</p>
		</#if>


		<div class="tutors clearfix row-fluid">
		<#list studentCourseDetails.personalTutors as relationship>

			<#assign personalTutor = relationship.agentMember />
			<div class="tutor clearfix span4">
				<#if !personalTutor??>
					${relationship.agentName} <span class="muted">External to Warwick</span>
					<#if can.do("Profiles.PersonalTutor.Update", profile) && acceptsPersonalTutorChanges>
						<a class="edit-tutor-link" href="<@routes.tutor_edit_no_tutor scjCode=studentCourseDetails.urlSafeId />"
						data-target="#modal-change-tutor"
						data-scj="${studentCourseDetails.scjCode}"
						>
						<i class="icon-edit"></i
						</a>
					</#if>
				<#else>

					<@fmt.relation_photo member relationship "tinythumbnail" />

					<h5>
						${personalTutor.fullName!"Personal tutor"}
						<#if can.do("Profiles.PersonalTutor.Update", profile) && acceptsPersonalTutorChanges>
							<a class="edit-tutor-link" href="<@routes.tutor_edit scjCode=studentCourseDetails.urlSafeId currentTutor=personalTutor/>"
							data-target="#modal-change-tutor"
							data-scj="${studentCourseDetails.scjCode}"
							>
							<i class="icon-edit"></i>
							</a>
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

		<#if studentCourseDetails.hasAPersonalTutor>
			<@meeting_macros.list studentCourseDetails tutorMeetings studentCourseDetails.personalTutors?first.relationshipType />
		</#if>
	<#else>
		<h4>Personal development</h4>
		<p class="text-warning"><i class="icon-warning-sign"></i> No personal development details are recorded in Tabula for the current year.</p>
	</#if>
</section>
</#escape>