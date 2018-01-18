<#import "*/_filters.ftl" as filters />

<#escape x as x?html>
	<div class="filters admin-assignment-submission-filters btn-group-group well well-sm" data-lazy="true">
		<@f.form commandName="submissionAndFeedbackCommand" action="${info.requestedUri.path}" method="GET" cssClass="form-inline filter-form">
			<@f.errors cssClass="error form-errors" />

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

			<button type="button" class="clear-all-filters btn btn-sm btn-filter">
				Clear filters
			</button>

		</@f.form>
	</div>
</#escape>