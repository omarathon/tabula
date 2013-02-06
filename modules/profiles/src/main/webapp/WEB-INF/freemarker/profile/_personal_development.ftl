<section id="personal-development" class="clearfix">
	<h4>Personal tutor</h4>
		<#if profile.personalTutor?is_string>
			<p>
				${profile.personalTutor}
				<#if !profile.personalTutor?string?starts_with("Not ")>
					<span class="muted">External to Warwick</span>
				</#if>
			</p>
		<#else>
			<div class="tutor">
				<div class="photo">
					<img src="<@routes.tutorPhoto profile />" />
				</div>
				<h5>${profile.personalTutor.fullName} &nbsp;
				<a id="edit-tutor-link" href="<@routes.tutor_edit studentUniId=profile.universityId tutor=profile.personalTutor/>"><i class="icon-edit"></i></a>
				</h5> 
				<#if profile.personalTutor.email??>
					<p><i class="icon-envelope"></i> <a href="mailto:${profile.personalTutor.email}">${profile.personalTutor.email}</a></p>
				</#if>
			</div>
		</#if>
</section>

