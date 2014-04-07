<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#assign finalMarkingStage = (allCompletedMarkerFeedback?? && allCompletedMarkerFeedback?size > 1)>

<#function markingId user>
	<#if !user.warwickId?has_content || user.getExtraProperty("urn:websignon:usersource")! == 'WarwickExtUsers'>
		<#return user.userId />
	<#else>
		<#return user.warwickId />
	</#if>
</#function>

<div class="content online-feedback">
	<#if command.submission??>
		<#assign submission = command.submission />
		<#include "_submission_summary.ftl">
	</#if>
	<#if (isMarking!false) && (isRejected!false)>
		<#include "_rejection_summary.ftl">
	</#if>
	<#if secondMarkerNotes?? && finalMarkingStage>
		<div class="well" >
			<h4>Notes from Second Marker</h4>
			<div style="margin-top: 0.3rem; max-width: 550px;" class="feedback-summary-comments">
				${secondMarkerNotes!""}
			</div>
		</div>
	</#if>
	<#assign submit_url>
		<#if isMarking!false>
			<@routes.markerOnlinefeedbackform assignment markingId(command.student) />
		<#else>
			<@routes.onlinefeedbackform assignment markingId(command.student) />
		</#if>
	</#assign>
	<@f.form cssClass="form-horizontal double-submit-protection"
			method="post" enctype="multipart/form-data" commandName="command" action="${submit_url}">

		<#if finalMarkingStage>
			<@form.row cssClass="alert alert-success" >
				<@form.label><i class="icon-lightbulb"></i> Populate with previous</@form.label>
				<@form.field>
					<#list allCompletedMarkerFeedback as feedback>
						<a data-feedback="${feedback.feedbackPosition.toString}" class="copyFeedback btn" title="Populate final feedback with ${feedback.feedbackPosition.description}">
							<i class="icon-arrow-down"></i>
							Copy ${feedback.feedbackPosition.description}
						</a>
					</#list>
				</@form.field>
			</@form.row>
		</#if>
		<#list assignment.feedbackFields as field>
			<div class="feedback-field">
				<#include "/WEB-INF/freemarker/submit/formfields/${field.template}.ftl">
			</div>
		</#list>

		<#if assignment.collectMarks>
			<@form.row>
				<@form.label path="mark">Mark</@form.label>
				<@form.field>
					<div class="input-append">
						<@f.input path="mark" cssClass="input-small" />
						<span class="add-on">%</span>
					</div>
					<@f.errors path="mark" cssClass="error" />
				</@form.field>
			</@form.row>
			<@form.row>
				<@form.label path="grade">Grade</@form.label>
				<@form.field>
					<@f.input path="grade" cssClass="input-small" />
					<@f.errors path="grade" cssClass="error" />
				</@form.field>
			</@form.row>
		</#if>

		<#if (allCompletedMarkerFeedback?? && allCompletedMarkerFeedback?size == 1)>
		<details class="control-group">
			<summary class="controls"><strong>Second Marker Notes </strong><span style="color: #606063;">add further comments about the first marker feedback</span></summary>
			<@form.labelled_row "rejectionComments" "Notes">


				<@f.textarea path="rejectionComments" cssClass="big-textarea" />
				<div style="margin: 0.5rem 0.5rem 0.5rem 0; width: 80%;" class="alert warning">Notes are not released to the student.</div>

			</@form.labelled_row>
		</details>

		</#if>

		<#if command.attachedFiles?has_content >
		<div class="feedbackAttachments">
			<@form.labelled_row "attachedFiles" "Attached files">
				<ul class="unstyled attachments">
					<#list command.attachedFiles as attachment>
						<li id="attachment-${attachment.id}" class="attachment">
							<i class="icon-file-alt"></i> <span>${attachment.name}</span>&nbsp;<i class="icon-remove-sign remove-attachment"></i>
							<@f.hidden path="attachedFiles" value="${attachment.id}" />
						</li>

					</#list>
				</ul>
			</@form.labelled_row>
		</div>
		<#else>
		<#-- Add invisible empty row for populating in case of copying files from a feedback further back in the workflow -->
		<div class="feedbackAttachments" style="display: none;">
			<@form.labelled_row "attachedFiles" "Attached files"><ul class="unstyled attachments"></ul></@form.labelled_row>
		</div>
		</#if>


		<@form.labelled_row "file.upload" "Attachments">
			<input type="file" name="file.upload" />
		</@form.labelled_row>

		<div class="submit-buttons">
			<input class="btn btn-primary" type="submit" value="Save">
			<a class="btn discard-changes" href="">Discard</a>
		</div>

	</@f.form>
</div>

