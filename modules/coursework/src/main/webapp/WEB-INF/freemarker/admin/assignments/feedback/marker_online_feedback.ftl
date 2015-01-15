<#if (allCompletedMarkerFeedback?size > 1) && !isCompleted><h2>Finalise feedback</h2></#if>
<#if command.submission?? && (allCompletedMarkerFeedback?size > 0)>
	<div class="content">
		<#assign submission = command.submission />
		<#include "_submission_summary.ftl">
	</div>
</#if>


<#assign isMarking=false />
<#assign widthClass><#if allCompletedMarkerFeedback?size &gt; 1>half-width</#if></#assign>

<div class="marker-feedback">
	<#list allCompletedMarkerFeedback as feedback>
		<div class="well ${widthClass}">
			<div class="feedback-summary-heading">
				<h3>
					<#if isModerated?? && isModerated && feedback.feedbackPosition.toString == "SecondFeedback">
						Moderation
					<#else>
						${feedback.feedbackPosition.description}
					</#if>
				</h3>
				<h5>${feedback.markerUser.fullName}</h5>
				<div class="clearfix"></div>
			</div>

			<#include "_feedback_summary.ftl">

			<#if (allCompletedMarkerFeedback?size > 1 && (feedback.hasComments || feedback.hasContent) && !isCompleted)>
				<div class="copy-comments">
					<a data-feedback="${feedback.feedbackPosition.toString}" class="copyFeedback btn btn-small btn-primary"><i class="icon-arrow-down"></i> Copy Comments</a>
				</div>
				<div class="clearfix"></div>
			</#if>

			<#if isModerated?? && !isModerated && secondMarkerNotes?? && feedback.feedbackPosition.toString == "SecondFeedback">
				<div class="feedback-notes alert-info">
					<h3>Notes from Second Marker</h3>
					<p>${secondMarkerNotes!""}</p>
				</div>
			</#if>
		</div>
	</#list>
</div>

<#assign isMarking=true />
<#if isCurrentUserFeedbackEntry>
	<div class="well">
		<h3>Final feedback</h3>
		<#include "online_feedback.ftl">
	</div>
</#if>