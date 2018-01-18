<#import "*/coursework_components.ftl" as components />
<#import "*/cm2_macros.ftl" as cm2 />
<#import "../_filters.ftl" as filters />
<#escape x as x?html>
	<#function route_function dept>
		<#local result><@routes.cm2.departmenthome dept academicYear /></#local>
		<#return result />
	</#function>
	<@cm2.departmentHeader "Assignments" department route_function academicYear "in" />

	<#-- Filtering -->
	<div class="fix-area">
		<div class="fix-header pad-when-fixed">
			<div class="filters admin-assignment-filters btn-group-group well well-sm" data-lazy="true">
				<@f.form commandName="command" action="${info.requestedUri.path}" method="GET" cssClass="form-inline">
					<@f.errors cssClass="error form-errors" />

					<#assign placeholder = "All modules" />
					<#assign modulesCustomPicker>
						<@bs3form.checkbox path="showEmptyModules">
							<@f.checkbox path="showEmptyModules" />
							Show modules with no filtered assignments
						</@bs3form.checkbox>

						<div class="module-search input-group">
							<input class="module-search-query module-picker module prevent-reload form-control" type="text" value="" placeholder="Search for a module" data-department="${department.code}" data-name="moduleFilters" data-wrap="true" />
							<span class="input-group-addon"><i class="fa fa-search"></i></span>
						</div>
					</#assign>
					<#assign currentfilter><@filters.current_filter_value "moduleFilters" placeholder; f>${f.module.code?upper_case}</@filters.current_filter_value></#assign>
					<@filters.filter name="module" path="command.moduleFilters" placeholder=placeholder currentFilter=currentfilter allItems=allModuleFilters customPicker=modulesCustomPicker; f>
						<input type="checkbox" name="${status.expression}"
									 value="${f.name}"
									 data-short-value="${f.description}"
						${filters.contains_by_filter_name(command.moduleFilters, f)?string('checked','')}>
						${f.description}
					</@filters.filter>

					<#assign placeholder = "All workflows" />
					<#assign currentfilter><@filters.current_filter_value "workflowTypeFilters" placeholder; f>${f.description}</@filters.current_filter_value></#assign>
					<@filters.filter "workflowType" "command.workflowTypeFilters" placeholder currentfilter allWorkflowTypeFilters; f>
						<input type="checkbox" name="${status.expression}"
									 value="${f.name}"
									 data-short-value="${f.description}"
						${filters.contains_by_filter_name(command.workflowTypeFilters, f)?string('checked','')}>
						${f.description}
					</@filters.filter>

					<#assign placeholder = "All assignment statuses" />
					<#assign currentfilter><@filters.current_filter_value "statusFilters" placeholder; f>${f.description}</@filters.current_filter_value></#assign>
					<@filters.filter "status" "command.statusFilters" placeholder currentfilter allStatusFilters; f>
						<input type="checkbox" name="${status.expression}"
									 value="${f.name}"
									 data-short-value="${f.description}"
						${filters.contains_by_filter_name(command.statusFilters, f)?string('checked','')}>
						${f.description}
					</@filters.filter>

					<@bs3form.labelled_form_group path="dueDateFilter.from" labelText="Assignment due date from">
						<div class="input-group">
							<@f.input type="text" path="dueDateFilter.from" cssClass="form-control date-picker input-sm" />
							<span class="input-group-addon"><i class="fa fa-calendar"></i></span>
						</div>
					</@bs3form.labelled_form_group>

					<@bs3form.labelled_form_group path="dueDateFilter.to" labelText="to">
						<div class="input-group">
							<@f.input type="text" path="dueDateFilter.to" cssClass="form-control date-picker input-sm" />
							<span class="input-group-addon"><i class="fa fa-calendar"></i></span>
						</div>
					</@bs3form.labelled_form_group>

					<button type="button" class="clear-all-filters btn btn-sm btn-filter">
						Clear filters
					</button>
				</@f.form>
			</div>
		</div>

		<div class="filter-results admin-assignment-list">
			<i class="fa fa-spinner fa-spin"></i> Loading&hellip;
		</div>
	</div>

</#escape>