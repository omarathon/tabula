<#if !isCompleted>
	<#include "online_feedback.ftl">
<#else>
	<#if command.submission??>
		<#assign submission = command.submission />
		<#include "_submission_summary.ftl">
	</#if>
	<#assign feedback = command.markerFeedback />
	<#include "_feedback_summary.ftl">
</#if>
