<#assign isMarking=false />
	<#list allCompletedMarkerFeedback as feedback>
		<div class="well">

			<h3>${feedback.getFeedbackPosition().description}</h3>
			<#assign feedback = feedback />
			<#include "_feedback_summary.ftl">
		</div>
	</#list>
	<#assign isMarking=true />
	<#if (isSecondMarker && isSecondMarkingPosition) || (isFirstMarker && (isFirstMarkingPosition || isThirdMarkingPosition))>
		<#include "online_feedback.ftl">
	</#if>

<#if isCompleted>
	<#if command.submission??>
		<#assign submission = command.submission />
		<#include "_submission_summary.ftl">
	</#if>
</#if>
