<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#escape x as x?html>
<h1>Extension settings for ${department.name}</h1>
<@f.form method="post" class="form-horizontal" action="/admin/department/${department.code}/settings" commandName="extensionSettingsCommand">
<#if features.extensions>
	<@form.row>
		<@form.label></@form.label>
		<@form.field>
			<label class="checkbox">
				<@f.checkbox path="allowExtensionRequests" id="allowExtensionRequests" />
				Allow students to request extensions
			</label>
			<@f.errors path="allowExtensionRequests" cssClass="error" />
			<div class="help-block">
				Students will be able to request extensions for this assignment via the submission page. If you select
				this option you will need to provide a link to your departments extension guidelines and/or provide a
				summary of those guidelines.
			</div>
		</@form.field>
	</@form.row>
	<div id="request-extension-fields" style="display:none;">
		<@form.row path="extensionGuidelineSummary">
			<@form.label for="extensionGuidelineSummary">Summary of extension guidelines:</@form.label>
			<@form.field>
				<@f.textarea path="extensionGuidelineSummary" class="big-textarea" />
				<div class="help-block">
					This should be a short outline of the departments extension guidelines. Use the field below to provide
					a link to the full guidelines. You can leave this blank if you wish. You can make a new paragraph by
					leaving a blank line (i.e. press Enter twice).
				</div>
			</@form.field>
		</@form.row>
		<@form.labelled_row "extensionGuidelineLink" "Link to full extension guidelines">
			<@f.input path="extensionGuidelineLink" cssClass="text big-input" />
			<div class="help-block">
				Copy the URL of a page or file containing your full extension guidelines and paste it here.
			</div>
		</@form.labelled_row>
	</div>
</#if>
	<div class="submit-buttons">
		<input type="submit" value="Save" class="btn btn-primary">
		or <a class="btn" href="<@routes.departmenthome department=department />">Cancel</a>
	</div>
</@f.form>
</#escape>