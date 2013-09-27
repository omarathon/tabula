<div class="form onlineFeedback">
	<form class="form-horizontal">
		<div class="feedback-field">
			<div class="control-group">
				<label class="control-label">Feedback</label>
				<div class="controls">
					<#list feedback.customFormValues as formValue>
						<textarea class="big-textarea" disabled="disabled">${formValue.value}</textarea>
					</#list>
				</div>
			</div>
		</div>

		<div class="control-group">
			<label class="control-label">Mark</label>
			<div class="controls">
				<div class="input-append">
					<input type="text" disabled="disabled" value="${feedback.mark}" class="input-small">
					<span class="add-on">%</span>
				</div>
			</div>
		</div>

		<div class="control-group">
			<label class="control-label">Grade</label>
			<div class="controls">
				<input type="text" disabled="disabled" value="${feedback.grade}" class="input-small">
			</div>
		</div>

		<#if feedback.attachments?has_content >
			<div class="control-group">
				<label class="control-label">Attachments</label>
				<div class="controls">
					<ul class="unstyled">
						<#list feedback.attachments as attachment>
							<li id="attachment-${attachment.id}">
								<span>${attachment.name}</span>
							</li>
						</#list>
					</ul>
				</div>
			</div>
		</#if>
	</form>
</div>
