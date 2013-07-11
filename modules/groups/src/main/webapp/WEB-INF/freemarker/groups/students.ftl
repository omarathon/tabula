<div class="modal-header">
	<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
	<h3>Students in ${command.group.name}</h3>
</div>

<div class="modal-body">

	<ul>
	<#list command.group.events as event>
	<#assign tutorUsers=event.tutors.users />
	<li>
		<div>
		<@fmt.weekRanges event />,
		${event.day.shortName} <@fmt.time event.startTime /> - <@fmt.time event.endTime />,
		${event.location!"[no location]"}
		</div>
		<div>

		<@fmt.p number=tutorUsers?size singular="Tutor" shownumber=false />:
		<#if !tutorUsers?has_content>
			<em>None</em>
		</#if>
		<#list tutorUsers as tutorUser>
			${tutorUser.fullName}<#if tutorUser_has_next>,</#if>
		</#list>
		</div>
	</li>
	</#list>
	</ul>

	<#if students?has_content>
	<ul class="profile-user-list">
	<#list students as student>
	<li>
		<div class="profile clearfix">
			<#if student.isMember()>
				<@fmt.member_photo student "tinythumbnail" false />
			<#else>
				<#-- `student` is actually only backed by a User here, so no Member photo, so let's explicitly serve the default photo -->
				<@fmt.member_photo {} "tinythumbnail" false />
			</#if>

			<div class="name">
				<h6>${student.fullName}</h6>
				${student.shortDepartment}
			</div>
		</div>
	</li>
	</#list>
	</ul>
	<#else>

	<p>No students have been allocated to this group.</p>

	</#if>

</div>