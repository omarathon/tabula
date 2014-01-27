<#escape x as x?html>
<#import "*/modal_macros.ftl" as modal />

	<#if isModal>
		<@modal.header>
			<h2>Attendance note for ${attendanceNote.student.fullName}</h2>
		</@modal.header>
	<#else>
		<h2>Attendance note for ${attendanceNote.student.fullName}</h2>
	</#if>

	<#assign content>
		<p>
			<#if attendance??>
				${attendance.state.description}:
			<#else>
				Not recorded:
			</#if>
			${attendanceNote.occurrence.event.group.groupSet.name},
			${attendanceNote.occurrence.event.group.name},
			${attendanceNote.occurrence.event.day.name} <@fmt.time attendanceNote.occurrence.event.startTime /> - <@fmt.time attendanceNote.occurrence.event.endTime />,
				Week ${attendanceNote.occurrence.week}
		</p>


		<#if attendanceNote.note?has_content>
			<#noescape>${attendanceNote.escapedNote}</#noescape>
		</#if>

		<#if attendanceNote.attachment?has_content>
			<p>
				<@fmt.download_link
					filePath="/note/${attendanceNote.student.universityId}/${attendanceNote.occurrence.id}/attachment/${attendanceNote.attachment.name}"
					mimeType=attendanceNote.attachment.mimeType
					title="Download file ${attendanceNote.attachment.name}"
					text="Download ${attendanceNote.attachment.name}"
				/>
			</p>
		</#if>

		<p class="hint">
			Attendance note updated
			<#if updatedBy?? && updatedBy?has_content>by ${updatedBy}, </#if>
			<#noescape>${updatedDate}</#noescape>
		</p>
	</#assign>

	<#if isModal>
		<@modal.body>
			<#noescape>${content}</#noescape>
		</@modal.body>

		<@modal.footer>
			<span class="submit-buttons">
				<#if can.do("SmallGroupEvents.Register", attendanceNote.student) >
					<a href="<@routes.editNote attendanceNote.student attendanceNote.occurrence />" class="btn btn-primary spinnable spinner-auto">
						Edit
					</a>
				</#if>
				<button class="btn" data-dismiss="modal" aria-hidden="true">Close</button>
			</span>
		</@modal.footer>

	<#else>

		<#noescape>${content}</#noescape>

		<#if can.do("SmallGroupEvents.Register", attendanceNote.student) >
			<a href="<@routes.editNote attendanceNote.student attendanceNote.occurrence />" class="btn btn-primary spinnable spinner-auto">Edit</a>
		</#if>

	</#if>

</#escape>