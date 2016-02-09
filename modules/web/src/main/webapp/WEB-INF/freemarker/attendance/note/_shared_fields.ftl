<@form.labelled_row "absenceType" "Absence type">
	<@f.select path="absenceType">
	<option value="" style="display: none;">Please select one&hellip;</option>
		<#list allAbsenceTypes as type>
			<@f.option value="${type.dbValue}" label="${type.description}" />
		</#list>
	</@f.select>
</@form.labelled_row>

<@form.labelled_row "note" "Note">
	<@f.textarea path="note" cssClass="input-block-level" rows="5" cssStyle="height: 150px;" />
</@form.labelled_row>

<#if command.attachedFile?has_content>
	<@form.labelled_row "attachedFile" "Attached file">
	<i class="icon-file-alt"></i>
		<@fmt.download_link
		filePath="/attendance/note/2013/${command.student.universityId}/${command.point.id}/attachment/${command.attachedFile.name}"
		mimeType=command.attachedFile.mimeType
		title="Download file ${command.attachedFile.name}"
		text="Download ${command.attachedFile.name}"
		/>
	&nbsp;
		<@f.hidden path="attachedFile" value="${command.attachedFile.id}" />
	<i class="icon-remove-sign remove-attachment"></i>

	<small class="subtle help-block">
		This is the file attached to this attendance note.
		Click the remove link to delete it.
	</small>

	</@form.labelled_row>

<script>
	jQuery(function($){
		$(".remove-attachment").on("click", function(e){
			$(this).closest('form').find('.attendance-file').show();
			$(this).closest(".control-group").remove()
			return false;
		});
	});
</script>
</#if>

<div class="attendance-file" <#if command.attachedFile?has_content>style="display:none;"</#if>>
<@form.filewidget basename="file" types=[] multiple=false />
</div>