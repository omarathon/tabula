<#escape x as x?html>
<section id="supervision" class="clearfix">
	<#if profile.supervisors?? && profile.supervisors?size gt 0>
		<h4>Supervisor<#if profile.supervisors?size gt 1>s</#if></h4>

		<div class="tutors clearfix row">
		<#list profile.supervisors as relationship>
			<#assign supervisor = relationship.agentMember />
			<div class="tutor clearfix span4">
				<#if supervisor??>
					<div class="photo">
						<img src="<@routes.relationshipPhoto profile relationship />" />
					</div>
					<h5>
						${supervisor.fullName!"Supervisor"}
					</h5>
					<#if supervisor.universityId == viewer.universityId>
						<span class="muted">(you)</span>
					<#else>
						<#if supervisor.email??>
							<p><i class="icon-envelope"></i> <a href="mailto:${supervisor.email}">${supervisor.email}</a></p>
						</#if>
					</#if>
				</#if>
			</div>
		</#list>
		</div>
	</#if>
</section>
</#escape>
