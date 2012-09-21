<#escape x as x?html>
<#compress>
	<#assign time_remaining=durationFormatter(assignment.closeDate) />
	<h1>Request an extension for <strong>${assignment.name}</strong></h1><br/>
	<p>
		This assignment closes on  <@fmt.date date=assignment.closeDate timezone=true /> (${time_remaining} remaining).
		To request an extension for this assignment please read the Extension Guidelines below and submit this form.
		You will receive a notification when your application has been processed.
	</p>
	<#if department.extensionGuidelineSummary??>
		<#include "/WEB-INF/freemarker/submit/formfields/guideline.ftl" >
	</#if>
	<#if department.extensionGuidelineLink??>
		<p>You should read the full <a href="${department.extensionGuidelineLink}">extension guidelines</a>
		before submitting your application for an extension.</p><br/>
	</#if>
	<@f.form method="post" enctype="multipart/form-data" class="form-horizontal" action="/module/${module.code}/${assignment.id}/extension" commandName="extensionRequestCommand">

		<@form.labelled_row "reason" "Please give a full statement of your reasons for applying for an extension">
			<@f.textarea path="reason" cssClass="text big-textarea" />
		</@form.labelled_row>

		<@form.labelled_row "requestedExpiryDate" "Until when do you request an extension?">
			<@f.input path="requestedExpiryDate" cssClass="date-time-picker" />
		</@form.labelled_row>

		<@form.labelled_row "file.upload" "Upload supporting documentation relevant to your request">
			<input type="file" name="file.upload" multiple />
			<div id="multifile-column-description" class="help-block">
				Your browser doesn't seem able to handle uploading multiple files<noscript>
				(or it does, but your browser is not running the Javascript needed to support it)
				</noscript>.
				A recent browser like Google Chrome or Firefox will be able to upload multiple files.
				You can still upload a single file here if you want.
				<div id="multifile-column-description-enabled" style="display:none">
					This uploader allows you to upload multiple files at once. They
					will need to be in the same folder on your computer for you to be
					able to select them all.
				</div>
			</div>
		</@form.labelled_row>

		<script>
			if (Supports.multipleFiles) {
				jQuery('#multifile-column-description')
					.html(jQuery('#multifile-column-description-enabled').html());
			}
		</script>

		<@form.row>
			<@form.label></@form.label>
			<@form.field>
				<label class="checkbox">
					<@f.checkbox path="readGuidelines" />
					I confirm that I have read the extension guidelines.
					</label>
					<@f.errors path="readGuidelines" cssClass="error" />
			</@form.field>
		</@form.row>

		<div class="submit-buttons">
			<input type="submit" class="btn btn-primary" value="Create" />
			or <a href=".." class="btn">Cancel</a>
		</div>

	</@f.form>
</#compress>
</#escape>