<#assign isMarker=false />
<#if !isCompleted>
	<#if !isFirstMarker>
		<div class="well">
			<h3>First markers feedback</h3>
			<#assign feedback = firstMarkerFeedback />
			<#include "_feedback_summary.ftl">
		</div>
	</#if>
	<#assign isMarker=true />
	<#include "online_feedback.ftl">
<#else>
	<#if command.submission??>
		<#assign submission = command.submission />
		<#include "_submission_summary.ftl">
	</#if>
	<#assign feedback = command.markerFeedback />
	<#include "_feedback_summary.ftl">
</#if>
