<#assign isMarking=false />
<#if !isCompleted || (isCompleted && secondMarkingComplete)>
	<#if !isFirstMarker || awaitingSecondMarking || (isFirstMarker && secondMarkingComplete)>
		<div class="well">
			<h3>First marker's feedback</h3>
			<#assign feedback = firstMarkerFeedback />
			<#include "_feedback_summary.ftl">
		</div>
	</#if>
	<#assign isMarking=true />
	<#if ((!isFirstMarker && !secondMarkingComplete) || (isFirstMarker && !awaitingSecondMarking && !secondMarkingComplete))>
	<#include "online_feedback.ftl">
	<#elseif secondMarkingComplete>
		<div class="well">
			<h3>Second marker's feedback</h3>
			<#assign feedback = secondMarkerFeedback />
			<#include "_feedback_summary.ftl">
		</div>
	</#if>
	<#if isFirstMarker && secondMarkingComplete>
		<div class="well">
			The new form goes in here
			<#include "online_feedback.ftl">
		</div>
	</#if>
<#else>
	<#if command.submission??>
		<#assign submission = command.submission />
		<#include "_submission_summary.ftl">
	</#if>
	<#assign feedback = command.markerFeedback />
	<#include "_feedback_summary.ftl">
</#if>
