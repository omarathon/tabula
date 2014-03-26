<#assign isMarking=false />
	<#if !isFirstMarkingPosition>
		<div class="well">
			<h3>First marker's feedback</h3>
			<#assign feedback = firstMarkerFeedback />
			<#include "_feedback_summary.ftl">
		</div>
	</#if>
	<#if isThirdMarkingPosition || (secondMarkerFeedback?has_content && !isSecondMarkingPosition)>
	<div class="well">
		<h3>Second marker's feedback</h3>
		<#assign feedback = secondMarkerFeedback />
		<#include "_feedback_summary.ftl">
	</div>
	</#if>
	<#assign isMarking=true />
	<#if (isSecondMarker && isSecondMarkingPosition) || (isFirstMarker && (isFirstMarkingPosition || isThirdMarkingPosition))>
		<#include "online_feedback.ftl">
	</#if>

<#if isCompleted>
	<#if command.submission??>
		<#assign submission = command.submission />
		<#include "_submission_summary.ftl">
	</#if>
	<#assign feedback = command.markerFeedback />
	<#include "_feedback_summary.ftl">
</#if>
