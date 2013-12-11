<div class="well alert alert-danger">
	<h3 style="color: inherit;">
		This feedback was rejected by the moderator at <@fmt.date secondMarkerFeedback.uploadedDate />
	</h3>

	<div class="form-horizontal">

		<div class="control-group">
			<label class="control-label">Comments</label>
			<div class="controls">
				<textarea class="big-textarea" disabled="disabled">${secondMarkerFeedback.rejectionComments!""}</textarea>
			</div>
		</div>

		<#if assignment.collectMarks>
			<div class="control-group">
				<label class="control-label">Adjusted Mark</label>
				<div class="controls">
					<div class="input-append">
						<input type="text" disabled="disabled" value="${secondMarkerFeedback.mark!""}" class="input-small">
						<span class="add-on">%</span>
					</div>
				</div>
			</div>

			<div class="control-group">
				<label class="control-label">Adjusted Grade</label>
				<div class="controls">
					<input type="text" disabled="disabled" value="${secondMarkerFeedback.grade!""}" class="input-small">
				</div>
			</div>
		</#if>
	</div>

</div>