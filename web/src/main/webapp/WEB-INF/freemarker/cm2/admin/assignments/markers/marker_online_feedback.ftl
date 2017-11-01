<#import "*/marking_macros.ftl" as marking_macros />

<div class="online-marking">
	<#if command.assignment.collectSubmissions || command.previousMarkerFeedback?has_content>
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
						<li role="presentation" <#if !command.assignment.collectSubmissions && stage_index == 0>class="active"</#if>>
							<a href="#${student.userId}${command.stage.name}${stage.name}" aria-controls="${student.userId}${stage.name}" role="tab" data-toggle="tab">${stage.description} feedback</a>
						</li>
					</#list>
				</#if>
			</ul>
			<div class="tab-content">
				<#if command.assignment.collectSubmissions>
					<div role="tabpanel" class="tab-pane active" id="${student.userId}${command.stage.name}submission">
						<#include "../feedback/_submission.ftl" />
					</div>
				</#if>
				<#if command.previousMarkerFeedback?has_content>
					<#include "_previous_feedback.ftl" />
				</#if>
			</div>
		</div>
	</#if>
	<#if command.currentMarkerFeedback?has_content>
		<#include "_marker_feedback.ftl" />
	</#if>
</div>




