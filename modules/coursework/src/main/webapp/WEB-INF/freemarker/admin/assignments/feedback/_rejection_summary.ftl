<#assign secondMarkerFeedback = parentFeedback.secondMarkerFeedback />
<div class="well alert alert-danger">
	<h3 style="color: inherit;">
		This feedback was rejected by the moderator at <@fmt.date secondMarkerFeedback.uploadedDate />
	</h3>

	<div class="${secondMarkerFeedback.feedbackPosition.toString} feedback-summary">

	<#if secondMarkerFeedback.mark?has_content || secondMarkerFeedback.grade?has_content>
		<div class="mark-grade" >
			<div >
				<div class="mg-label" >
					Mark:</div>
				<div >
					<span class="mark">${secondMarkerFeedback.mark!""}</span>
					<span>%</span>
				</div>
				<div class="mg-label" >
					Grade:</div>
				<div class="grade">${secondMarkerFeedback.grade!""}</div>
			</div>
		</div>
	<#else>
		<h5>No mark or grade added.</h5>
	</#if>

	<div class="feedback-comments">
	<#list secondMarkerFeedback.customFormValues as formValue>
		<#if formValue.value?has_content>
			<h5>Feedback Comments</h5>
		</#if>
		<div>
		${formValue.value!"<h5>No feedback comments added.</h5>"}
		</div>
	</#list>
	</div>

	<#if secondMarkerFeedback.attachments?has_content >
		<div class="feedback-attachments attachments" >
			<h5>Attachments</h5>
			<div>
				<#assign downloadMFUrl><@routes.markerFeedbackFilesDownload secondMarkerFeedback/></#assign>
				<@fmt.download_attachments feedback.attachments downloadMFUrl "for ${feedback.feedbackPosition.description?uncap_first}" "feedback-${feedback.feedback.universityId}" />
				<#list feedback.attachments as attachment>
					<input value="${attachment.id}" name="${attachment.name}" type="hidden"/>
				</#list>
			</div>
		</div>
	</#if>

	<div style="clear: both;"></div>
	</div>

</div>