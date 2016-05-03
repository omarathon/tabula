<#escape x as x?html>
	<#macro member_note_list memberNotes>
		<#local canCreateMemberNote = can.do("MemberNotes.Create", profile) />

		<div class="list-controls">
			<#if canCreateMemberNote>
				<a class="btn btn-link create"
					data-toggle="modal"
					data-target="#note-modal"
					href="#note-modal"
					data-url="<@routes.profiles.create_member_note profile/>"
					title="Create new note"
				>
					New administrative note
				</a>
			</#if>
			<a class="toggle-all-details btn btn-link open-all-details" title="Expand all notes">Expand all</a>
			<a class="toggle-all-details btn btn-link close-all-details" title="Collapse all notes">Collapse all</a>
		</div>


		<#list memberNotes as note>
			<#local canDeletePurgeMemberNote = can.do("MemberNotes.Delete", note) />
			<#local canEditMemberNote = can.do("MemberNotes.Update", note) />

			<#local deleteTools =" disabled" />
			<#local deleted ="" />
			<#local nonDeleteTools ="" />
			<#local attachments = note.attachments?? && note.attachments?size gt 0 />

			<#if note.deleted>
				<#local deleteTools = "" />
				<#local deleted ="deleted subtle" />
				<#local nonDeleteTools =" disabled" />
			</#if>

			<details class="${deleted}">
				<summary>
					<div class="detail-arrow-fix">
						<div class="row">
							<div class="col-md-2">
								<@fmt.date date=note.creationDate includeTime=false shortMonth=true />
							</div>
							<div class="col-md-6">
								${note.title!} <#if attachments><i class="fa fa-paperclip"></i></#if>
							</div>
							<#if canEditMemberNote>
								<div class="col-md-4 member-note-toolbar">
									<a data-toggle="modal" data-target="#note-modal" href="#note-modal" data-url="<@routes.profiles.edit_member_note note />" class="btn btn-link edit${nonDeleteTools}" title="Edit note">Edit</a>
									<#if canDeletePurgeMemberNote>
										<a href="<@routes.profiles.delete_member_note note />" class="btn btn-link delete${nonDeleteTools}" title="Delete note">Delete</a>
										<a href="<@routes.profiles.restore_member_note note />" class="btn btn-link restore${deleteTools}" title="Restore note">Restore</a>
										<a href="<@routes.profiles.purge_member_note note />" class="btn btn-link purge${deleteTools}" title="Purge note">Purge</a>
									</#if>
									<i class="fa fa-spinner fa-spin"></i>
								</div>
							</#if>
						</div>
					</div>
				</summary>

				<div class="description">
					<#if note.note??>
						<#noescape>${note.escapedNote}</#noescape>
					</#if>
					<#if attachments >
						<@fmt.display_deleted_attachments note.attachments note.deleted?string("","hidden") />
						<#assign mbDownloadUrl><@routes.profiles.download_member_note_attachment note /></#assign>
						<div class="deleted-files ${note.deleted?string('hidden','')}"><@fmt.download_attachments note.attachments mbDownloadUrl /></div>
					</#if>
					<small class="very-subtle clearfix">Student note created by ${note.creator.fullName}, <@fmt.date note.lastUpdatedDate /></small>
				</div>
			</details>
		</#list>
	</#macro>
</#escape>
