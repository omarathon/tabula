<section id="membernote-details" class="clearfix membernote">
	<h4>Student Notes</h4>
	<#if RequestParameters.action?? && RequestParameters.action == "noteAdded">
		<div class="alert alert-success">
			<button type="button" class="close" data-dismiss="alert">&times;</button>
			<p>A note was added to <strong>${profile.firstName}'s</strong> profile.</p>
		</div>
	</#if>

	<@member_note_list memberNotes />

	<#escape x as x?html>
		<#macro member_note_list memberNotes>
			<a style="float: left; text-decoration: none;" class="btn-like membernote_create" data-toggle="modal" data-target="#note-modal" href="<@routes.create_member_note profile/>" title="Create a new student note"><i class="icon-edit"></i> New student note</a>
				<a style="float: right; cursor: pointer;" class="toggle-all-details btn-like open-all-details" title="Expand all notes"><i class="icon-plus"></i> Expand all</a>
				<a style="float: right; cursor: pointer;" class="toggle-all-details btn-like close-all-details hide" title="Collapse all notes"><i class="icon-minus"></i> Collapse all</a>

			<div style="clear: both; padding-top: 10px;">
			<#list memberNotes as note>

				<details class="memberNote" style="margin: 5px 0 10px 0">
					<summary style="border-bottom: dotted 1px #dcdcdd;">
						<span class="date" style="display: inline-block; width: 180px; font-weight: bold;"><@fmt.date date=note.creationDate includeTime=false /></span>
						<span class="title" style="display: inline-block; width: 210px; font-weight: bold;">${note.title!}</title></span>
					</summary>
					<#if note.note??>
						<div class="description" style="padding: 15px;">
							${note.note}
						</div>
					</#if>
					 <#if note.attachments??>
						<@fmt.download_attachments note.attachments "/notes/${note.id}/" />
					</#if>
				</details>


			</#list>

			</div>


		</#macro>


	</#escape>


</section>

