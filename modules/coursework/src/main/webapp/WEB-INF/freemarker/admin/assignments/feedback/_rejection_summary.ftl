<#assign secondMarkerFeedback = parentFeedback.secondMarkerFeedback />
<div class="well alert alert-danger">
	<h3 style="color: inherit;">
		This feedback was rejected by the moderator at <@fmt.date secondMarkerFeedback.uploadedDate />
	</h3>

	<div class="${secondMarkerFeedback.feedbackPosition.toString}" style="padding: 0 1rem 0rem 1rem;">

	<#if secondMarkerFeedback.mark?has_content || secondMarkerFeedback.grade?has_content>
		<div style='margin: 0.5rem 0 0.5rem 0;
				font-size: 22px;
				font-weight: normal;
				font-style: normal;
				letter-spacing: normal;
				font-family: Bitter, Cambria, "Liberation Serif", Georgia, "Times New Roman", Times, serif;
				line-height: 1.4em;
				'>
			<div style="display:table-row; width: 600px; padding: 1rem 0 2rem 0;">
				<div style="display:table-cell; padding-right: 0.5rem;">Mark:</div><div style="padding-right: 3rem;"><span class="mark">${secondMarkerFeedback.mark!""}</span><span>%</span></div>
				<div style="display:table-cell; padding-right: 0.5rem;">Grade:</div><div class="grade">${secondMarkerFeedback.grade!""}</div>
			</div>
		</div>
	</#if>
		<div style="float: left; max-width:550px; margin: 0 1.5rem 1.5rem 0;" >
			<h5>Feedback Comments</h5>
			<div style="margin-top: 0.3rem;" class="feedback-summary-comments">
			<#list secondMarkerFeedback.customFormValues as formValue>
					${formValue.value!""}
				</#list>
			</div>
		</div>
		<#if secondMarkerFeedback.attachments?has_content >
			<div style="float: left; width: 260px;">
				<h5>Attachments</h5>
				<div style="display: table-row; margin-bottom: 0.5rem;">
					<@fmt.download_attachments secondMarkerFeedback.attachments "/admin/module/${secondMarkerFeedback.feedback.assignment.module.code}/assignments/${secondMarkerFeedback.feedback.assignment.id}/marker/${secondMarkerFeedback.feedback.id}/download/" />
				</div>
			</div>
		</#if>

		<div style="clear: both;"></div>
	</div>

</div>