<#escape x as x?html>
<#import "*/modal_macros.ftl" as modal />
<@modal.wrapper isModal!false 'modal-lg'>
	<#if success??>
		<#assign isAdd = !command.note?has_content && !command.attachment?has_content && !command.absenceType?has_content />
		<div
			class="attendance-note-success"
			data-linkid="bulk"
			data-state="<#if isAdd>Add<#else>Edit</#if>"
		></div>
	</#if>

	<#assign heading>
		<#if command.isNew()>
			<h3 class="modal-title">Create attendance note for <@fmt.p command.students?size "student" /></h3>
		<#else>
			<h3 class="modal-title">Edit attendance note for <@fmt.p command.students?size "student" /></h3>
		</#if>
	</#assign>

	<#if isModal!false>
		<@modal.header>
			<#noescape>${heading}</#noescape>
		</@modal.header>
	<#else>
		<div id="container">
	</#if>

	<#if isModal!false>

		<@modal.body />

		<@modal.footer>
		<form class="double-submit-protection">
			<span class="submit-buttons">
				<button class="btn btn-primary spinnable" type="submit" name="submit" data-loading-text="Saving&hellip;">
					Save
				</button>
				<button class="btn btn-default" data-dismiss="modal" aria-hidden="true">Cancel</button>
			</span>
		</form>
		</@modal.footer>
	<#else>
		<div id="container">
			<#if isAuto!false>
				<div class="alert alert-warning">
					Points marked as Missed (authorised) must have an attendance note explaining why the absence was authorised.
				</div>
			</#if>

			<@f.form id="bulk-attendance-note-form" method="post" enctype="multipart/form-data" action="" commandName="command" class="form-horizontal double-submit-protection">
				<#include "_shared_fields.ftl" />

				<@form.labelled_row "overwrite" "">
					<label class="checkbox">
						<@f.checkbox path="overwrite" />
						Overwrite existing attendance notes
					</label>
				</@form.labelled_row>

				<input type="hidden" name="isSave" value="true" />
		</@f.form>
		</div>
	</#if>

	<#if !isModal!false>
		</div><#-- #container -->
	</#if>
</@modal.wrapper>
</#escape>