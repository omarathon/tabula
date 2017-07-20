<#import "*/coursework_components.ftl" as components />
<#import "*/cm2_macros.ftl" as cm2 />

<#escape x as x?html>
	<#if assignment.collectSubmissions>
		<#assign title="Submissions and feedback" />
	<#else>
		<#assign title="Feedback" />
	</#if>
	<@cm2.assignmentHeader title assignment "for" />

	<#if assignment.openEnded>
		<p class="dates">
			<@fmt.interval assignment.openDate />, never closes
			(open-ended)
			<#if !assignment.opened>
				<span class="label label-info">Not yet open</span>
			</#if>
		</p>
	<#else>
		<p class="dates">
			<@fmt.interval assignment.openDate assignment.closeDate />
			<#if assignment.closed>
				<span class="label label-info">Closed</span>
			</#if>
			<#if !assignment.opened>
				<span class="label label-info">Not yet open</span>
			</#if>
		</p>
	</#if>

	<#-- Filtering -->
	<div class="fix-area form-post-container">
		<div class="fix-header pad-when-fixed">
			<#include "_filter.ftl" />

			<#assign currentView = "summary" />
			<#include "_action-bar.ftl" />
		</div>

		<div class="filter-results admin-assignment-submission-list">
			<i class="fa fa-spinner fa-spin"></i> Loading&hellip;
		</div>
	</div>
</#escape>