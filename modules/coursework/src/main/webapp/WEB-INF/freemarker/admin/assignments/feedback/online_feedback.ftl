<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#assign markingStage = (allCompletedMarkerFeedback?? && allCompletedMarkerFeedback?size > 0)>

<#function markingId user>
	<#if !user.warwickId?has_content || user.getExtraProperty("urn:websignon:usersource")! == 'WarwickExtUsers'>
		<#return user.userId />
	<#else>
		<#return user.warwickId! />
	</#if>
</#function>

<div class="content online-feedback feedback-summary">

	<#if command.submission?? && !markingStage>
		<#assign submission = command.submission />
		<#include "_submission_summary.ftl">
	</#if>
	<#if (isMarking!false) && (isRejected!false)>
		<#include "_rejection_summary.ftl">
	</#if>
	<#assign submit_url>
		<#if isMarking!false>
			<@routes.markerOnlinefeedbackform assignment markingId(command.student) marker/>
		<#else>
			<@routes.onlinefeedbackform assignment markingId(command.student) />
		</#if>
	</#assign>
	<@f.form cssClass="form-horizontal double-submit-protection"
			method="post" enctype="multipart/form-data" commandName="command" action="${submit_url}">

		<@f.errors cssClass="error form-errors" />

		<div class="alert alert-success hide"></div>
		<#list assignment.feedbackFields as field>
			<div class="feedback-field">
				<#assign showHelpText = true>
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
				<#if isGradeValidation>
					<span class="auto-grade" id="auto-grade-${markingId(command.student)}" data-marking-id="${markingId(command.student)}">
						${command.grade}
					</span>
					<script>
						jQuery(function($){
							var $gradeSpan = $('#auto-grade-${markingId(command.student)}')
								, $markInput = $gradeSpan.closest('form').find('input[name=mark]')
								, currentRequest = null
								, doRequest = function(){
									if (currentRequest != null) {
										currentRequest.abort();
									}
									currentRequest = $.post('<@routes.generateGradesForMarks command.assignment />',{
										'studentMarks' : {
											'${markingId(command.student)}' : $markInput.val()
										}
									}, function(data){
										if (data['${markingId(command.student)}']) {
											$gradeSpan.html(data['${markingId(command.student)}']);
										}
									});
								};
							$markInput.on('keyup', doRequest);
						});
					</script>
				<#else>
					<@form.field>
						<@f.input path="grade" cssClass="input-small" />
						<@f.errors path="grade" cssClass="error" />
					</@form.field>
				</#if>
			</@form.row>
		</#if>

		<#if (allCompletedMarkerFeedback?? && allCompletedMarkerFeedback?size == 1)>
		<details class="control-group second-marker-notes">
			<summary class="controls"><strong>Second Marker Notes </strong><span>add further comments about the first marker feedback</span></summary>
			<div>
			<div class="control-group">
				<label class="control-label"></label>
				<div class="controls">
					<div class="alert warning marker-notes-warn">
							Notes are not released to the student.
					</div>
				</div>
			</div>

			<@form.labelled_row "rejectionComments" "Notes">
				<@f.textarea path="rejectionComments" cssClass="big-textarea" />
			</@form.labelled_row>
			</div>
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
			<input type="file" name="file.upload" multiple/>
			<div id="multifile-column-description" class="help-block">
				<#include "/WEB-INF/freemarker/multiple_upload_help.ftl" />
			</div>
		</@form.labelled_row>

		<div class="submit-buttons">
			<input class="btn btn-primary" type="submit" value="Save">
			<a class="btn discard-changes" href="">Cancel</a>
		</div>

	</@f.form>
</div>

