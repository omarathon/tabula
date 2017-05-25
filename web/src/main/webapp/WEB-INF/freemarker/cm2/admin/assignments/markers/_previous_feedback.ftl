<#assign isMarking = command.currentMarkerFeedback?has_content />
<#assign stages = command.previousMarkerFeedback?keys />
<#list stages as stage>
	<#assign markerFeedback = mapGet(command.previousMarkerFeedback, stage) />
	<div role="tabpanel" class="tab-pane previous-marker-feedback" id="${student.userId}${command.stage.name}${stage.name}">
		<@markerFeedbackSummary markerFeedback stage />
	</div>
</#list>

<#macro markerFeedbackSummary feedback stage>

		<h4>${stage.description} <#if feedback.marker??>- ${feedback.marker.fullName}</#if></h4>

		<#list feedback.customFormValues as formValue>
			<#if formValue.value?has_content>
				<@bs3form.form_group><textarea class="form-control feedback-comments" readonly="readonly">${formValue.value!""}</textarea></@bs3form.form_group>
			<#else>
				<p>No feedback comments added.</p>
			</#if>
		</#list>

		<div class="row form-inline">
			<#if feedback.mark?has_content || feedback.grade?has_content>
					<div class="col-xs-3">
						<label>Mark</label>
						<div class="input-group">
							<input type="text" class="form-control" readonly="readonly" value="${feedback.mark!""}">
							<div class="input-group-addon">%</div>
						</div>
					</div>

					<div class="col-xs-3">
						<label>Grade</label>
						<input type="text" class="form-control" readonly="readonly" value="${feedback.grade!""}">
					</div>
			<#else>
				<div class="col-xs-6"><span>No mark or grade added.</span></div>
			</#if>

			<div class="col-xs-6">
				<#-- Download a zip of all feedback or just a single file if there is only one -->
				<#if feedback.attachments?has_content >
					<#local attachment = "" />
					<#if !feedback.attachments?is_enumerable>
					<#-- assume it's a FileAttachment -->
						<#local attachment = feedback.attachments />
					<#elseif feedback.attachments?size == 1>
					<#-- take the first and continue as above -->
						<#local attachment = feedback.attachments?first />
					</#if>
					<#if attachment?has_content>
						<#local downloadUrl><@routes.cm2.downloadMarkerFeedbackOne command.assignment command.marker feedback attachment /></#local>
					<#elseif feedback.attachments?size gt 1>
						<#local downloadUrl><@routes.cm2.downloadMarkerFeedbackAll command.assignment command.marker feedback stage.description+" feedback" /></#local>
					</#if>
					<a class="btn btn-default long-running use-tooltip" href="${downloadUrl}">Download feedback</a>
					<ul class="feedback-attachments hide">
						<#list feedback.attachments as attachment>
							<li id="attachment-${attachment.id}" class="attachment">
								<span>${attachment.name}</span>&nbsp;<a href="#" class="remove-attachment">Remove</a>
								<input type="hidden" name="attachedFiles" value="${attachment.id}" />
							</li>
						</#list>
					</ul>
				</#if>

				<#if isMarking>
						<a class="copy-feedback btn btn-default long-running use-tooltip" href="#">Copy comments and files</a>
				</#if>
			</div>

		</div>

</#macro>