<#import "../related_students/meeting/meeting_list_macros.ftl" as meeting_macros />

<#escape x as x?html>
<section id="supervision" class="clearfix">
	<#if studentCourseDetails.supervisors?? && studentCourseDetails.supervisors?size gt 0>
		<h4>Supervision</h4>

		<div class="tutors clearfix row-fluid">
		<#list studentCourseDetails.supervisors as relationship>
			<#assign supervisor = relationship.agentMember />
			<div class="tutor clearfix span4">
				<#if supervisor??>

					<@fmt.relation_photo member relationship "tinythumbnail" />

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
		<#assign relType = (studentCourseDetails.supervisors?first).relationshipType />
		<@meeting_macros.list studentCourseDetails supervisorMeetings relType />
	<#else>
		<h4>Supervisors</h4>
		<p class="text-warning"><i class="icon-warning-sign"></i> No supervision details are recorded in Tabula for the current year.</p>
	</#if>
</section>
</#escape>
