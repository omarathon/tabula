<#escape x as x?html>
<#import "*/modal_macros.ftl" as modal />

	<#if success??>
		<#assign isAdd = !command.attendanceNote.note?has_content && !command.attendanceNote.attachment?has_content && !command.attendanceNote.absenceType?has_content />
		<div
			class="attendance-note-success"
			data-linkid="#attendanceNote-${student.universityId}-${command.occurrence.id}"
			data-state="<#if isAdd>Add<#else>Edit</#if>"
		></div>
	</#if>

	<#assign heading>
		<#if command.isNew()>
			<h2>Create attendance note for ${student.fullName}</h2>
		<#else>
			<h2>Edit attendance note for ${student.fullName}</h2>
		</#if>
	</#assign>

	<#if isModal!false>
		<@modal.header>
			<#noescape>${heading}</#noescape>
		</@modal.header>
	<#elseif isIframe>
		<div id="container">
	<#else>
		<#noescape>${heading}</#noescape>
	</#if>

	<#if isModal!false>
		<@modal.body />

		<@modal.footer>
			<form class="double-submit-protection">
				<span class="submit-buttons">
					<button class="btn btn-primary spinnable" type="submit" name="submit" data-loading-text="Saving&hellip;">
						Save
					</button>
					<button class="btn" data-dismiss="modal" aria-hidden="true">Cancel</button>
				</span>
			</form>
		</@modal.footer>
	<#else>

		<p>
			<#if command.customState??>
				${command.customState.description}:
			<#elseif command.attendance??>
				${command.attendance.state.description}:
			<#else>
				Not recorded:
			</#if>
			${command.occurrence.event.group.groupSet.name},
			${command.occurrence.event.group.name},
			${command.occurrence.event.day.name} <@fmt.time command.occurrence.event.startTime /> - <@fmt.time command.occurrence.event.endTime />,
			Week ${command.occurrence.week}

			<#if command.customState?? && command.attendance?? && command.customState.dbValue != command.attendance.state.dbValue>
				<small class="subtle help-block">
					This attendance has not yet been saved.
				</small>
			</#if>
		</p>

		<@f.form id="attendance-note-form" method="post" enctype="multipart/form-data" action="" commandName="command" class="form-horizontal double-submit-protection">

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
						filePath="/profiles/note/${command.student.universityId}/${command.occurrence.id}/attachment/${command.attachedFile.name}"
						mimeType=command.attachedFile.mimeType
						title="Download file ${command.attachedFile.name}"
						text="Download ${command.attachedFile.name}"
					/>
					&nbsp;
					<@f.hidden path="attachedFile" value="${command.attachedFile.id}" />
					<i class="icon-remove-sign remove-attachment"></i>

					<small class="subtle help-block">
						This is the file attached to this attendance note.
						Click the remove link next to a document to delete it.
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

			<#if !isIframe>

				<div class="form-actions">
					<div class="pull-right">
						<input type="submit" value="Save" class="btn btn-primary" data-loading-text="Saving&hellip;" autocomplete="off">
						<a class="btn" href="${returnTo}">Cancel</a>
					</div>
				</div>

			</#if>

		</@f.form>

	</#if>

	<#if isIframe>
		</div> <#--container -->
	</#if>

</#escape>