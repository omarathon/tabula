<#include "../_filters.ftl" />
<#escape x as x?html><#compress>
	<h1>Extensions</h1>

	<div class="filters btn-group-group well well-small well-sm">
		<#assign formAction><@routes.cm2.filterExtensions /></#assign>
		<@f.form
			method="GET"
			action="${formAction}"
			commandName="command"
		>
			<button type="button" class="clear-all btn btn-xs">Clear all filters</button>

			<#-- Department filter-->
			<#assign placeholder = "All departments" />
			<#assign currentfilter><@current_filter_value "command.departments" placeholder; department>${department.code}</@current_filter_value></#assign>
			<@filter "command.departments" placeholder currentfilter command.allDepartments; department>
				<input type="checkbox" name="${status.expression}" value="${department.code}" data-short-value="${department.name}" ${contains_by_code(command.departments, department)?string('checked','')}>
				${department.name}
			</@filter>

			<#-- Module filter-->
			<#assign placeholder = "All modules" />
			<#assign currentfilter><@current_filter_value "command.modules" placeholder; module>${module.code}</@current_filter_value></#assign>
			<@filter "command.modules" placeholder currentfilter command.allModules; module>
				<input type="checkbox" name="${status.expression}" value="${module.code}" data-short-value="${module.code}" ${contains_by_code(command.modules, module)?string('checked','')}>
				${module.code}
			</@filter>

			<#-- State filter-->
			<#assign placeholder = "All request states" />
			<#assign currentfilter><@current_filter_value "command.states" placeholder; state>${state.description}</@current_filter_value></#assign>
			<@filter "command.states" placeholder currentfilter command.allStates; state>
				<input type="checkbox" name="${status.expression}" value="${state.dbValue}" data-short-value="${state.description}" ${contains_by_db_value(command.states, state)?string('checked','')}>
				${state.description}
			</@filter>

			<#-- Time filter-->
			<#assign placeholder = "All dates" />
			<#assign currentfilter><@current_filter_value "command.times" placeholder; time>${time.code}</@current_filter_value></#assign>
			<@filter "command.times" placeholder currentfilter command.allTimes; time>
				<input type="checkbox" name="${status.expression}" value="${time.code}" data-short-value="${time.code}" ${contains_by_code(command.times, time)?string('checked','')}>
				${time.code}
			</@filter>

		</@f.form>
	</div>
	<div class="filter-results">
		<#include "_filter_results.ftl" />
	</div>

</#compress></#escape>
