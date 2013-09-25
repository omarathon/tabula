<#if memberNoteSuccess??>
<#else>
	<@f.form id="edit-member-note-form" method="post" enctype="multipart/form-data" action="" commandName="command" class="form-horizontal double-submit-protection">

		<@form.labelled_row "title" "Title">
			<@f.input type="text" path="title" cssClass="input-block-level" maxlength="255" />
		</@form.labelled_row>

		<@form.labelled_row "note" "Note">
			<@f.textarea path="note" cssClass="input-block-level" rows="5" cssStyle="height: 150px;" />
		</@form.labelled_row>

		<#if command.attachedFiles?has_content  && edit?? >
			<@form.labelled_row "attachedFiles" "Attached files">
			<ul class="unstyled">
				<#list command.attachedFiles as attachment>
					<li id="attachment-${attachment.id}" class="attachment">
						<i class="icon-file-alt"></i><span> ${attachment.name}</span>&nbsp;
						<@f.hidden path="attachedFiles" value="${attachment.id}" />
						<i class="icon-remove-sign remove-attachment"></i>
					</li>
				</#list>
			</ul>
			<script>
				jQuery(function($){
					$(".remove-attachment").on("click", function(e){
						$(this).closest("li.attachment").remove();
						return false;
					});
				});
			</script>
			<small class="subtle help-block">
				This is a list of file attachments for this administrative note.
				Click the remove link next to a document to delete it.
			</small>
		</@form.labelled_row>
		</#if>

		<#assign fileTypes=command.attachmentTypes />
		<@form.filewidget basename="file" types=fileTypes />

	</@f.form>

</#if>