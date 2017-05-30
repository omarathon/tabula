<#import "/WEB-INF/freemarker/marking_macros.ftl" as marking_macros />

<#function markingId user>
	<#if !user.warwickId?has_content || user.getExtraProperty("urn:websignon:usersource")! == 'WarwickExtUsers'>
		<#return user.userId />
	<#else>
		<#return user.warwickId! />
	</#if>
</#function>

<div class="online-marking">
	<div class="details">
		<ul class="nav nav-tabs" role="tablist">
		<#if command.assignment.collectSubmissions>
			<li role="presentation" class="active">
				<a href="#${student.userId}${command.stage.name}submission" aria-controls="${student.userId}submission" role="tab" data-toggle="tab">Submission details</a>
			</li>
		</#if>
		<#if command.previousMarkerFeedback?has_content>
			<#assign stages=command.previousMarkerFeedback?keys />
			<#list stages as stage>
				<li role="presentation">
					<a href="#${student.userId}${command.stage.name}${stage.name}" aria-controls="${student.userId}${stage.name}" role="tab" data-toggle="tab">${stage.description} feedback</a>
				</li>
			</#list>
		</#if>
		</ul>
		<div class="tab-content">
		<#if command.assignment.collectSubmissions>
			<#include "_submission.ftl" />
		</#if>
		<#if command.previousMarkerFeedback?has_content>
			<#include "_previous_feedback.ftl" />
		</#if>
		</div>
	</div>
	<#if command.currentMarkerFeedback?has_content>
		<#include "_marker_feedback.ftl" />
	</#if>
</div>




