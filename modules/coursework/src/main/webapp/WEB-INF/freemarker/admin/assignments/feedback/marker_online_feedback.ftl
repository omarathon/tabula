<#if (allCompletedMarkerFeedback?size > 1  && isCurrentUserFeedbackEntry)>
	<div class="well" style="padding: 1rem;">
		<h2>Finalise feedback</h2>
		<#assign isMarking=true />
		<#include "online_feedback.ftl">
	</div>
</#if>

<#assign isMarking=false />
<#list allCompletedMarkerFeedback as feedback>
	<div class="well" style="padding: 1em;">
		<h3>${feedback.feedbackPosition.description} (${feedback.markerUser.fullName})</h3>
		<#include "_feedback_summary.ftl">
	</div>
</#list>

<#assign isMarking=true />
<#if isCurrentUserFeedbackEntry && allCompletedMarkerFeedback?size < 2>
	<#include "online_feedback.ftl">
</#if>

<#if isCompleted>
	<#if command.submission??>
		<#assign submission = command.submission />
		<#include "_submission_summary.ftl">
	</#if>
</#if>
