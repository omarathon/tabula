<#macro feedbackSummary feedback isModerated showDates=false>
	<#assign assignment=feedback.assignment />
	<#assign markerfeedbacks=feedback />
	<#if (assignment.cm2Assignment)>
	//FIXME - need to replace this with CM2 equivalent to "SecondFeedback" feedbackPosition
		<#if isModerated?has_content && isModerated && markerfeedbacks.outstandingStages[0].name == "SecondFeedback">
		<div class="markerfeedbacks-summary-heading">
			<h3>Moderation</h3>
			<#if assignment.id?has_content>

				<#if (markerfeedbacks.outstandingStages[0].name == "single-marking-completed") >
				${markerfeedbacks.outstandingStages[0].name}
				</#if>
				<h5>${markerfeedbacks.markerFeedback[0].marker.fullName} <#if showDates && markerfeedbacks.uploadedDate?has_content><small>- <@fmt.date markerfeedbacks.uploadedDate /></small></#if></h5>
			</#if>

			<div class="clearfix"></div>
		</div>
		<div class="<${markerfeedbacks.outstandingStages[0].name} markerfeedbacks-summary" >
			<div class="markerfeedbacks-details">
				<#if markerfeedbacks.rejectionComments?has_content>
					<div class="markerfeedbacks-comments">
						<h5>Feedback Comments</h5>

						<p>${markerfeedbacks.rejectionComments}</p>
					</div>
				<#else>
					<div class="markerfeedbacks-comments"><h5>No markerfeedbacks comments added.</h5></div>
				</#if>
			</div>
		</div>
		</#if>

	<#else>
		<#if markerfeedbacks.feedbackPosition?has_content>
			<#if isModerated?has_content && isModerated && markerfeedbacks.feedbackPosition.toString == "SecondFeedback">
			<div class="markerfeedbacks-summary-heading">
				<h3>Moderation</h3>
				<#if markerfeedbacks.markerUser?has_content>
					<h5>${markerfeedbacks.markerUser.fullName} <#if showDates><small>- <@fmt.date markerfeedbacks.uploadedDate /></small></#if></h5>
				</#if>
				<div class="clearfix"></div>
			</div>
			<div class="${markerfeedbacks.feedbackPosition.toString} markerfeedbacks-summary" >
				<div class="markerfeedbacks-details">
					<#if markerfeedbacks.rejectionComments?has_content>
						<div class="markerfeedbacks-comments">
							<h5>Feedback Comments</h5>

							<p>${markerfeedbacks.rejectionComments}</p>
						</div>
					<#else>
						<div class="markerfeedbacks-comments"><h5>No markerfeedbacks comments added.</h5></div>
					</#if>
				</div>
			</div>
			<#else>
			<div class="markerfeedbacks-summary-heading">
				<#if markerfeedbacks.feedbackPosition?has_content>
					<h3>${markerfeedbacks.feedbackPosition.description}</h3>
				</#if>
				<#if markerfeedbacks.markerUser?has_content>
					<h5>${markerfeedbacks.markerUser.fullName} <#if showDates><small>- <@fmt.date markerfeedbacks.uploadedDate /></small></#if></h5>
				</#if>
				<div class="clearfix"></div>
			</div>
				<#if assignment.cm2Assignment?has_content>
				<div class="${markerfeedbacks.feedbackPosition.toString} markerfeedbacks-summary" >
				<div class="markerfeedbacks-details">
					<#if markerfeedbacks.mark?has_content || markerfeedbacks.grade?has_content>
						<div class="mark-grade" >
							<div>
								<div class="mg-label" >
									Mark:</div>
								<div>
									<span class="mark">${markerfeedbacks.mark!""}</span>
									<span>%</span>
								</div>
								<div class="mg-label" >
									Grade:</div>
								<div class="grade">${markerfeedbacks.grade!""}</div>
							</div>
						</div>
					<#else>
						<h5>No mark or grade added.</h5>
					</#if>
				</#if>
				<#if markerfeedbacks.customFormValues?has_content>
					<#list markerfeedbacks.customFormValues as formValue>
						<#if formValue.value?has_content>
							<div class="markerfeedbacks-comments">
								<h5>Feedback comments</h5>
								<p>${formValue.valueFormattedHtml!""}</p>
							</div>
						<#else>
							<h5>No markerfeedbacks comments added.</h5>
						</#if>
					</#list>
				</#if>
			</div>

				<#if markerfeedbacks.attachments?has_content >
					<div class="markerfeedbacks-attachments attachments">
						<h5>Attachments</h5>
						<div>
							<#assign downloadMFUrl><@routes.cm2.markerFeedbackFilesDownload markerfeedbacks/></#assign>
							<@fmt.download_attachments markerfeedbacks.attachments downloadMFUrl "for ${markerfeedbacks.feedbackPosition.description?uncap_first}" "markerfeedbacks-${markerfeedbacks.markerfeedbacks.studentIdentifier}" />
							<#list markerfeedbacks.attachments as attachment>
								<input value="${attachment.id}" name="${attachment.name}" type="hidden"/>
							</#list>
						</div>
					</div>
				</#if>
				<div style="clear: both;"></div>
			</div>
			</#if>
		</#if>
	</#if>
</#macro>

<#macro secondMarkerNotes feedback isModerated>
	<#assign assignment=feedback.assignment />
	<#assign markerfeedbacks=feedback />
	<#if (assignment.cm2Assignment)>
	<#else>
		<#if isModerated?? && !isModerated && markerfeedbacks.allMarkerFeedback.rejectionComments?? && markerfeedbacks.allMarkerFeedback.feedbackPosition.toString == "SecondFeedback">
		<div class="markerfeedbacks-notes alert alert-info">
			<h3>Notes from Second Marker</h3>
			<p>${markerfeedbacks.allMarkerFeedback.rejectionComments!""}</p>
		</div>
		</#if>
	</#if>
</#macro>