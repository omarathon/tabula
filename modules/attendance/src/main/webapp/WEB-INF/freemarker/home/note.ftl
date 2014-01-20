<#escape x as x?html>
<#import "*/modal_macros.ftl" as modal />

	<#if isModal>
		<@modal.header>
			<h2>Edit attendance note</h2>
		</@modal.header>
	<#elseif isIframe>
		<div id="container">
	<#else>
		<h2>Edit attendance note</h2>
	</#if>

	<#if isModal>
		<@modal.body />

		<@modal.footer>
			<form class="double-submit-protection">
				<span class="submit-buttons">
					<button class="btn btn-primary spinnable spinner-auto" type="submit" name="submit" data-loading-text="Saving&hellip;">
						Save
					</button>
					<button class="btn" data-dismiss="modal" aria-hidden="true">Cancel</button>
				</span>
			</form>
		</@modal.footer>
	<#else>

		<@f.form id="attendance-note-form" method="post" enctype="multipart/form-data" action="" commandName="command" class="form-horizontal double-submit-protection">

			<@form.labelled_row "note" "Note">
				<@f.textarea path="note" cssClass="input-block-level" rows="5" cssStyle="height: 150px;" />
			</@form.labelled_row>

			<#if isIframe>
				<input type="hidden" name="isModal" value="true" />
			<#else>

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