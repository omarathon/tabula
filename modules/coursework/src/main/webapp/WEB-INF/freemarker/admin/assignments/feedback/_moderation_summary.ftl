<#if command.approved>
	<div class="well alert alert-success">
		<h3 style="color: inherit;">You approved this feedback at <@fmt.date completedDate /></h3>
	</div>
<#else>
	<#assign moderationFeedback = firstMarkerFeedback.feedback.secondMarkerFeedback />
	<div class="well alert alert-danger">
		<h3 style="color: inherit;">You rejected this feedback at <@fmt.date completedDate /></h3>
	</div>

	<div class="${moderationFeedback.feedbackPosition.toString}" style="padding: 0 1rem 0rem 1rem;">

		<#if moderationFeedback.mark?has_content || moderationFeedback.grade?has_content>
			<div style='margin: 0.5rem 0 0.5rem 0;
				font-size: 22px;
				font-weight: normal;
				font-style: normal;
				letter-spacing: normal;
				font-family: Bitter, Cambria, "Liberation Serif", Georgia, "Times New Roman", Times, serif;
				line-height: 1.4em;
				'>
				<div style="display:table-row; width: 600px; padding: 1rem 0 2rem 0;">
					<div style="display:table-cell; padding-right: 0.5rem;">Mark:</div><div style="padding-right: 3rem;"><span class="mark">${moderationFeedback.mark!""}</span><span>%</span></div>
					<div style="display:table-cell; padding-right: 0.5rem;">Grade:</div><div class="grade">${moderationFeedback.grade!""}</div>
				</div>
			</div>
		</#if>
		<div style="float: left; max-width:550px; margin: 0 1.5rem 1.5rem 0;" >
			<h5>Feedback Comments</h5>
			<div style="margin-top: 0.3rem;" class="feedback-summary-comments">
				<#list moderationFeedback.customFormValues as formValue>
					${formValue.value!""}
				</#list>
			</div>
		</div>
		<#if moderationFeedback.attachments?has_content >
			<div style="float: left; width: 260px;">
				<h5>Attachments</h5>
				<div style="display: table-row; margin-bottom: 0.5rem;">
					<@fmt.download_attachments moderationFeedback.attachments "/admin/module/${moderationFeedback.feedback.assignment.module.code}/assignments/${moderationFeedback.feedback.assignment.id}/marker/${moderationFeedback.feedback.id}/download/" />
				</div>
			</div>
		</#if>

		<div style="clear: both;"></div>
	</div>
</#if>