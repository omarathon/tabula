<div class="${feedback.feedbackPosition.toString} feedback-summary" >
	<div class="feedback-details" style="float: left; max-width: 550px;">
		<#if feedback.mark?has_content || feedback.grade?has_content>
			<div class="mark-grade" >
				<div>
					<div class="mg-label" >
						Mark:</div>
					<div >
						<span class="mark">${feedback.mark!""}</span>
						<span>%</span>
					</div>
					<div class="mg-label" >
						Grade:</div>
					<div class="grade">${feedback.grade!""}</div>
				</div>
			</div>
		<#else>
			<h5>No mark or grade added.</h5>
		</#if>

		<#list feedback.customFormValues as formValue>
			<#if formValue.value?has_content>
		<div class="feedback-comments" style="clear: left;">
				<h5>Feedback Comments</h5>
			<#else>
		<div>
			</#if>
			${formValue.value!"<h5>No feedback comments added.</h5>"}
		</#list>
		</div>
	</div>





		<#if feedback.attachments?has_content >
		<div class="feedback-attachments attachments">
			<h5>Attachments</h5>
			<div>
				<#assign downloadMFUrl><@routes.markerFeedbackFilesDownload feedback/></#assign>
				<@fmt.download_attachments feedback.attachments downloadMFUrl "for ${feedback.feedbackPosition.description?uncap_first}" "feedback-${feedback.feedback.universityId}" />
				<#list feedback.attachments as attachment>
					<input value="${attachment.id}" name="${attachment.name}" type="hidden"/>
				</#list>
			</div>
		</div>
		</#if>



	<div style="clear: both;"></div>

</div>