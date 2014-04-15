
<#if (allCompletedMarkerFeedback?size > 1) && !isCompleted><h2>Finalise feedback</h2></#if>
<#if command.submission?? && (allCompletedMarkerFeedback?size > 0)>
	<div class="content">
		<#assign submission = command.submission />
		<#include "_submission_summary.ftl">
	</div>
</#if>


<#assign isMarking=false />
<#list allCompletedMarkerFeedback as feedback>
	<div class="well">
		<div class="feedback-summary-heading">
			<h3>
				<#if isModerated?? && isModerated && feedback.feedbackPosition.toString == "SecondFeedback">
					Moderation
				<#else>
					${feedback.feedbackPosition.description}
				</#if>
				(${feedback.markerUser.fullName})
			</h3>
		<#if (allCompletedMarkerFeedback?size > 1 && (feedback.hasComments || feedback.hasContent) && !isCompleted)>
			<div>
				<a data-feedback="${feedback.feedbackPosition.toString}" class="copyFeedback btn btn-primary"><i class="icon-arrow-down"></i> Copy</a>
			</div>
		</#if>
		<div style="clear: both;"></div>
		</div>
		<#include "_feedback_summary.ftl">
	</div>
</#list>

<#if isModerated?? && !isModerated>
	<#if secondMarkerNotes??>
		<div class="well" >
			<div class="feedback-notes">
				<h4>Notes from Second Marker</h4>
			${secondMarkerNotes!""}
			</div>
		</div>
	</#if>
</#if>

<#assign isMarking=true />
<#if isCurrentUserFeedbackEntry>
	<#include "online_feedback.ftl">
</#if>

<#if (allCompletedMarkerFeedback?size > 1) && !isCompleted>
	<#if command.submission??>
		<#assign submission = command.submission />
		<#include "_submission_summary.ftl">
	</#if>
</#if>
