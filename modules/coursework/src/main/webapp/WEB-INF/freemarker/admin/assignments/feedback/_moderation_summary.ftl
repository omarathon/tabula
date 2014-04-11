<#if command.approved>
	<div class="well alert alert-success">
		<h3 style="color: inherit;">You approved this feedback at <@fmt.date completedDate /></h3>
	</div>
<#else>
	<#assign moderationFeedback = firstMarkerFeedback.feedback.secondMarkerFeedback />
	<div class="well alert alert-danger">
		<h3 style="color: inherit;">You rejected this feedback at <@fmt.date completedDate /></h3>
	</div>

	<div class="${moderationFeedback.feedbackPosition.toString} feedback-summary">

		<#if moderationFeedback.mark?has_content || moderationFeedback.grade?has_content>
			<div class="mark-grade" >
				<div >
					<div class="mg-label" >
						Mark:</div>
					<div >
						<span class="mark">${moderationFeedback.mark!""}</span>
						<span>%</span>
					</div>
					<div class="mg-label" >
						Grade:</div>
					<div class="grade">${moderationFeedback.grade!""}</div>
				</div>
			</div>
		</#if>
		<div class="feedback-comments">
			<#list moderationFeedback.customFormValues as formValue>
				<#if formValue.value?has_content>
					<h5>Feedback Comments</h5>
				</#if>
				<div>
					${formValue.value!"<h5>No feedback comments added.</h5>"}
				</div>
			</#list>
		</div>
		<#if moderationFeedback.attachments?has_content >
			<div class="feedback-attachments">
				<h5>Attachments</h5>
				<div>
					<#assign downloadMFUrl><@routes.markerFeedbackFilesDownload moderationFeedback/></#assign>
					<@fmt.download_attachments feedback.attachments downloadMFUrl "" "feedback-${feedback.feedback.universityId}" />
				</div>
			</div>
		</#if>

		<div style="clear: both;"></div>
	</div>
</#if>