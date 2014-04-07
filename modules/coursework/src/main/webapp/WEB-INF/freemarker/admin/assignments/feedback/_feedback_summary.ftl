<div class="${feedback.feedbackPosition.toString}" style="padding: 0 1rem 0rem 1rem; background-color: #f5f5f5;">

	<#if feedback.mark?has_content || feedback.grade?has_content>
		<div style='margin: 0.5rem 0 0.5rem 0;
		font-size: 22px;
		font-weight: normal;
		font-style: normal;
		letter-spacing: normal;
		font-family: Bitter, Cambria, "Liberation Serif", Georgia, "Times New Roman", Times, serif;
		line-height: 1.4em;
		'>
			<div style="display:table-row; width: 600px; padding: 1rem 0 2rem 0;">
				<div style="display:table-cell; padding-right: 0.5rem;">Mark:</div><div style="padding-right: 3rem;"><span class="mark">${feedback.mark!""}</span><span>%</span></div>
				<div style="display:table-cell; padding-right: 0.5rem;">Grade:</div><div class="grade">${feedback.grade!""}</div>
			</div>
		</div>
	</#if>
	<div style="float: left; max-width:550px; margin: 0 1.5rem 1.5rem 0;" >
		<h5>Feedback Comments</h5>
		<div style="margin-top: 0.3rem;" class="feedback-summary-comments">
			<#list feedback.customFormValues as formValue>
				${formValue.value!""}
			</#list>
		</div>
	</div>


	<#if feedback.attachments?has_content >
		<div style="float: left; width: 260px;" class="attachments">
			<h5>Attachments</h5>
			<div style="display: table-row; margin-bottom: 0.5rem;">
				<#assign downloadMFUrl><@routes.markerFeedbackFilesDownload feedback/></#assign>
				<@fmt.download_attachments feedback.attachments downloadMFUrl "for ${feedback.feedbackPosition.description?uncap_first}" "feedback-${feedback.feedback.universityId}" />
				<#list feedback.attachments as attachment>
					<input value="${attachment.id}" name="${attachment.name}" type="hidden"/>
				</#list>
			</div>
		</div>
	</#if>
	<div style="clear: both;"></div>
	<#if feedback.rejectionComments??>
		<div>
			<h4>Notes from Second Marker</h4>
			<div style="margin-top: 0.3rem; max-width: 550px;" class="feedback-summary-notes">
			${feedback.rejectionComments!""}
			</div>
		</div>
	</#if>

</div>
