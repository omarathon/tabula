<#import "*/_filters.ftl" as filters />

<#import "*/submission_components.ftl" as components />
<#import "*/cm2_macros.ftl" as cm2 />

<#escape x as x?html>
	<h1>${assignment.name} (${assignment.module.code?upper_case})</h1>

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
			<div class="filters admin-assignment-submission-filters btn-group-group well well-sm" data-lazy="true">

				<@f.form commandName="submissionAndFeedbackCommand" action="${info.requestedUri.path}" method="GET" cssClass="form-inline filter-form">
					<@f.errors cssClass="error form-errors" />
					<button type="button" class="clear-all-filters btn btn-link">
								<span class="fa-stack">
									<i class="fa fa-filter fa-stack-1x"></i>
									<i class="fa fa-ban fa-stack-2x"></i>
								</span>
					</button>

					<#assign placeholder = "All submission states" />
					<#assign currentfilter><@filters.current_filter_value "submissionStatesFilters" placeholder; f>${f.description}</@filters.current_filter_value></#assign>
					<@filters.filter "submission-states" "submissionAndFeedbackCommand.submissionStatesFilters" placeholder currentfilter allSubmissionStatesFilters; f>
						<input type="checkbox" name="${status.expression}"
									 value="${f.name}"
									 data-short-value="${f.description}"
						${filters.contains_by_filter_name(submissionAndFeedbackCommand.submissionStatesFilters, f)?string('checked','')}>
					${f.description}
					</@filters.filter>

					<#assign placeholder = "All  plagiarism statuses" />
					<#assign currentfilter><@filters.current_filter_value "plagiarismFilters" placeholder; f>${f.description}</@filters.current_filter_value></#assign>
					<@filters.filter "plagiarism-status" "submissionAndFeedbackCommand.plagiarismFilters" placeholder currentfilter allPlagiarismFilters; f>
						<input type="checkbox" name="${status.expression}"
									 value="${f.name}"
									 data-short-value="${f.description}"
						${filters.contains_by_filter_name(submissionAndFeedbackCommand.plagiarismFilters, f)?string('checked','')}>
					${f.description}
					</@filters.filter>
					<#assign placeholder = "All  statuses" />
					<#assign currentfilter><@filters.current_filter_value "statusesFilters" placeholder; f>${f.description}</@filters.current_filter_value></#assign>
					<@filters.filter "other-statuses" "submissionAndFeedbackCommand.statusesFilters" placeholder currentfilter allStatusFilters; f>
						<input type="checkbox" name="${status.expression}"
									 value="${f.name}"
									 data-short-value="${f.description}"
						${filters.contains_by_filter_name(submissionAndFeedbackCommand.statusesFilters, f)?string('checked','')}>
					${f.description}
					</@filters.filter>

					<div class='plagiarism-filter' style="display: none;">
						<@bs3form.labelled_form_group path="overlapFilter.min" labelText="Min overlap">
							<div class="input-group">
								<@f.input type="text" path="overlapFilter.min" cssClass="form-control input-sm" type="number" min="0" max="100" />
								<span class="input-group-addon">%</span>
							</div>
						</@bs3form.labelled_form_group>

						<@bs3form.labelled_form_group path="overlapFilter.max" labelText="Max overlap">
							<div class="input-group">
								<@f.input type="text" path="overlapFilter.max" cssClass="form-control input-sm" type="number" min="0" max="100" />
								<span class="input-group-addon">%</span>
							</div>
						</@bs3form.labelled_form_group>
					</div>
				</@f.form>
			</div>
		</div>

		<#assign currentView = "table" />
		<#include "_action-bar.ftl" />

		<div class="filter-results admin-assignment-submission-list">
			<i class="fa fa-spinner fa-spin"></i> Loading&hellip;
		</div>
	</div>

</#escape>

