<#assign stages=command.previousMarkerFeedback?keys />
<#list stages as stage>
	<#assign markerFeedback = mapGet(command.previousMarkerFeedback, stage) />
	<div role="tabpanel" class="tab-pane" id="${student.userId}${stage.name}">
		<@markerFeedbackSummary markerFeedback stage.allocationName />
	</div>
</#list>

<#macro markerFeedbackSummary feedback stageName>

		<#list feedback.customFormValues as formValue>
			<#if formValue.value?has_content>
				<@bs3form.form_group><textarea class="form-control" readonly="readonly">${formValue.value!""}</textarea></@bs3form.form_group>
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
				<div class="col-xs-6"><p>No mark or grade added.</p></div>
			</#if>

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
				<#local downloadUrl><@routes.cm2.downloadMarkerFeedbackAll command.assignment command.marker feedback stageName+" feedback" /></#local>
			</#if>
			<div class="col-xs-3">
				<a class="btn btn-default long-running use-tooltip" href="${downloadUrl}">Download feedback</a>
			</div>
		</#if>

		</div>

</#macro>