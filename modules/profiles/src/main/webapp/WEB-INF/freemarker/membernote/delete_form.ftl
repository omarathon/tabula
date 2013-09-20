<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>

	<div class="modal-header">
		<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
		<h3>Delete student note for ${command.member.fullName}</h3>
	</div>

	<div class="modal-body">


		<div class="alert">
			<p>Are you sure you want to delete the following student note (including attachments created <strong><@fmt.date date=command.memberNote.creationDate includeTime=false capitalise=false /></strong>?</p>
			<p>Deleted notes and attachments will no longer be visible by the student.</p>
		</div>
		<div style="padding: 20px; border: 1px solid silver;" class="input-block-level">
			<#if command.memberNote.title??>
				<div style="margin-bottom: 15px;"><strong>${command.memberNote.title}</strong></div>
			</#if>
			<#if command.memberNote.note??>
				<div style="margin-bottom: 15px;">${command.memberNote.note}</div>
			</#if>
			<#if (command.memberNote.attachments?size > 0)>
				<div>
				<#list command.memberNote.attachments as attached>
					<i class="icon-file-alt"></i> ${attached.name}
				</#list>
			</#if>
			</div>

		</div>

		</summary>
</div>

<div class="modal-footer">
	<input id="member-note-delete" type="submit" class="btn btn-primary" value="Confirm">
</div>
