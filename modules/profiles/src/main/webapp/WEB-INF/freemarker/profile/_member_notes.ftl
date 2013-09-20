<section id="membernote-details" class="clearfix membernote">
	<h4>Administrative notes</h4>
	<#if RequestParameters.action?? && RequestParameters.action == "noteAdded">
		<div class="alert alert-success">
			<button type="button" class="close" data-dismiss="alert">&times;</button>
			<p>A note was added to <strong>${profile.firstName}'s</strong> profile.</p>
		</div>
	</#if>

	<@member_note_list memberNotes />

	<#escape x as x?html>
		<#macro member_note_list memberNotes>


			<a style="float: right; text-decoration: none; margin-left: 5px; display: block;" class="btn-like membernote_create" data-toggle="modal" data-target="#note-modal" href="#note-modal" data-url="<@routes.create_member_note profile/>" title="Create a new student note"><i class="icon-edit"></i> New administrative note</a>
			<a style="float: right; cursor: pointer;" class="toggle-all-details btn-like open-all-details" title="Expand all notes"><i class="icon-plus"></i> Expand all</a>
			<a style="float: right; cursor: pointer;" class="toggle-all-details btn-like close-all-details hide" title="Collapse all notes"><i class="icon-minus"></i> Collapse all</a>

			<div style="clear: both; padding-top: 10px;">
			<#list memberNotes as note>

				<#local deleteTools =" disabled" />
				<#local deleted ="" />
				<#local nonDeleteTools ="" />
				<#local attachments = note.attachments?? && note.attachments?size gt 0 />

				<#if note.deleted>
					<#local deleteTools = "" />
					<#local deleted ="deleted muted" />
					<#local nonDeleteTools =" disabled" />
				</#if>

				<details class="${deleted}" style="margin: 10px 0 10px 0;">
					<summary style="border-bottom: dotted 1px #dcdcdd;">
						<div style="display: inline-block; width: 95%; margin-bottom: -7px;">

							<div class="member-note-toolbar" style="display: inline-block; float: right; width: 65px;">
								<a data-toggle="modal" data-target="#note-modal" href="#note-modal" data-url="<@routes.edit_member_note note />" class="btn-like edit${nonDeleteTools}" title="Edit note"><i class="icon-edit" ></i></a>
								<a href="<@routes.delete_member_note note />" class="btn-like delete${nonDeleteTools}" title="Delete note"><i class="icon-trash"></i></a>
								<a href="<@routes.restore_member_note note />" class="btn-like restore${deleteTools}" title="Restore note"><i class="icon-repeat"></i></a>
								<a href="<@routes.purge_member_note note />" class="btn-like purge${deleteTools}" title="Purge note"><i class="icon-remove"></i></a>
								<i class="icon-spinner icon-spin"></i>
							</div>

							<div style="display: table-row">
								<div class="date" style="display: table-cell; vertical-align: bottom; width: 140px; font-weight: bold;"><@fmt.date date=note.creationDate includeTime=false shortMonth=true /><div style="clear: both;"></div></div>
								<div class="title" style="display: table-cell; font-weight: bold; padding-right: 5px;">${note.title!} <#if attachments><i class="icon-paper-clip"></i></#if></div>
							</div>


						</div>
						<div style="clear: both;"></div>
					</summary>
					<div class="description" style="padding: 15px;">
					<#if note.note??>

							${note.note}

					</#if>
					 <#if attachments >
						 <@display_deleted_attachments note.attachments note.deleted?string("","hidden") />
						 <div class="deleted-files ${note.deleted?string('hidden','')}"><@fmt.download_attachments note.attachments "/notes/${note.id}/" /></div>
					</#if>
					<small class="muted clearfix" style="padding-top: 10px;">Student note created by ${note.creator.fullName}, <@fmt.date note.lastUpdatedDate /></small>
					</div>
				</details>


			</#list>

			</div>


		</#macro>

		   <#macro display_deleted_attachments attachments visible="">

			   <ul class="deleted-files ${visible}">
				<#list attachments as files>
					<li class="muted deleted"><i class="icon-file-alt"></i> ${files.name}</li>
				</#list>
				</ul>
		   </#macro>


	</#escape>


</section>


