<#escape x as x?html>
<#compress>
	<#assign time_remaining=durationFormatter(assignment.closeDate) />
	<h1>Request an extension for <strong>${assignment.name}</strong></h1>
	<#if isClosed && !isModification>
		<p>
			This assignment closed on <@fmt.date date=assignment.closeDate timezone=true /> (${time_remaining}).
			You cannot request an extension after the close date has passed.
		</p>
	<#else>

		<#if isModification>
			<#if existingRequest.approved>
				<div class="alert alert-success">
					<#assign approved_ago=durationFormatter(existingRequest.approvedOn) />
					Your extension request was approved - <@fmt.date date=existingRequest.approvedOn timezone=true /> (${approved_ago}).
				</div>
			<#elseif existingRequest.rejected>
				<div class="alert alert-error">
					Your extension request has been rejected.
				</div>
			<#else>
				<div class="alert alert-info">Your extension request is being reviewed.</div>
			</#if>
			<#if existingRequest.expiryDate?? || existingRequest.approvalComments??>
				<div class="form-horizontal">
					<#if existingRequest.expiryDate?? && existingRequest.approved>
						<div class="control-group">
							<label class="control-label">New submission deadline</label>
							<div class="controls">
								<p>
									Your new submission deadline is <@fmt.date date=existingRequest.expiryDate at=true/>
								</p>
							</div>
						</div>
					</#if>
					<#if existingRequest.approvalComments??>
						<div class="control-group">
							<label class="control-label">Review comments</label>
							<div class="controls"><p>${existingRequest.approvalComments}</p></div>
						</div>
					</#if>
				</div>
				<hr/>
			</#if>
			<#assign time_since_request=durationFormatter(existingRequest.requestedOn) />
			<p>
				You requested an extension for this assignment on <@fmt.date date=existingRequest.requestedOn timezone=true /> (${time_since_request}).
				Use the form below to update the details of your extension request. If you have been asked to provide
				additional information about your request or for supporting documentation you can add them here.
			</p>
		<#else>
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
				before submitting your application for an extension.</p>
			</#if>
		</#if>

		<@f.form method="post" enctype="multipart/form-data" class="form-horizontal" action="${url('/module/${module.code}/${assignment.id}/extension')}" commandName="extensionRequestCommand">

			<#if isModification>
				<@f.hidden path="modified" value="true" />
			</#if>

			<@form.labelled_row "reason" "Please give a full statement of your reasons for applying for an extension">
				<@f.textarea path="reason" cssClass="text big-textarea" />
			</@form.labelled_row>

			<@form.labelled_row "requestedExpiryDate" "Requested extension date">
				<@f.input path="requestedExpiryDate" cssClass="date-time-picker" />
			</@form.labelled_row>

			<@form.labelled_row "file.upload" "Upload supporting documentation relevant to your request">
				<input type="file" name="file.upload" />
				<div id="multifile-column-description" class="help-block">
					<#include "/WEB-INF/freemarker/multiple_upload_help.ftl" />
				</div>
			</@form.labelled_row>

			<#if command.attachedFiles?has_content >
				<@form.labelled_row "attachedFiles" "Supporting documents">
				<ul>
					<#list command.attachedFiles as attachment>
						<li id="attachment-${attachment.id}" class="attachment">
							<a href="<@routes.extensionrequestattachment assignment=assignment filename=attachment.name />">
								${attachment.name}
							</a>&nbsp;
							<@f.hidden path="attachedFiles" value="${attachment.id}" />
							<a class="remove-attachment" href="">Remove</a>
						</li>
					</#list>
				</ul>
				<script>
					jQuery(function($){
						$(".remove-attachment").on("click", function(e){
							e.preventDefault();
							$(this).closest("li.attachment").remove();
						});
					});
				</script>
				<div class="help-block">
					This is a list of all supporting documents that have been attached to this extension request. Click the
					remove link next to a document to delete it.
				</div>
				</@form.labelled_row>
			</#if>

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
				<input type="submit" class="btn btn-primary" value="Submit" />
				or <a href=".." class="btn">Cancel</a>
			</div>
		</@f.form>
	</#if>
</#compress>
</#escape>