
<#--	<form class="form-horizontal">
		<div class="feedback-field">
			<div class="control-group">
				<label class="control-label">Feedback</label>
				<div class="controls">
					<#list feedback.customFormValues as formValue>
						<textarea class="big-textarea" disabled="disabled">${formValue.value!""}</textarea>
					</#list>
				</div>
			</div>
		</div>

		<div class="control-group">
			<label class="control-label">Mark</label>
			<div class="controls">
				<div class="input-append">
					<input type="text" disabled="disabled" value="${feedback.mark!""}" class="input-small">
					<span class="add-on">%</span>
				</div>
			</div>
		</div>

		<div class="control-group">
			<label class="control-label">Grade</label>
			<div class="controls">
				<input type="text" disabled="disabled" value="${feedback.grade!""}" class="input-small">
			</div>
		</div>

		<#if feedback.attachments?has_content >
			<div class="control-group">
				<label class="control-label">Attachments</label>
				<div class="controls">
					<a class="btn long-running" href="<@routes.markerFeedbackFiles assignment feedback/>">
						<i class="icon-download"></i>
						<@fmt.p number=feedback.attachments?size singular="file" />
					</a>
				</div>
			</div>
		</#if>

	</form>
</div>-->


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
			<div style="float: left; width: 260px;">
				<h5>Attachments</h5>
				<div style="display: table-row; margin-bottom: 0.5rem;">
					<@fmt.download_attachments feedback.attachments "/admin/module/${feedback.feedback.assignment.module.code}/assignments/${feedback.feedback.assignment.id}/marker/${feedback.feedback.id}/download/" />
				</div>
			</div>
		</#if>

		 <div style="clear: both;"></div>
	</div>


