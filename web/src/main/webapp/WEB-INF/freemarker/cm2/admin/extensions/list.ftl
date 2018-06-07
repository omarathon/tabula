<#import "*/cm2_macros.ftl" as cm2 />
<#include "../_filters.ftl" />
<#assign currentPage = command.page />
<#assign totalPages = results.total/command.extensionsPerPage?ceiling />

<#escape x as x?html><#compress>
	<h1>Extensions</h1>
	<div class="filters btn-group-group well well-small well-sm">
		<#assign formAction><@routes.cm2.filterExtensions academicYear /></#assign>
		<@f.form
			method="GET"
			action="${formAction}"
			commandName="command"
			class="form-inline"
		>
			<#-- Department filter-->
			<#assign placeholder = "All departments" />
			<#assign currentfilter><@current_filter_value "command.departments" placeholder; department>${department.code}</@current_filter_value></#assign>
			<@filter "departments" "command.departments" placeholder currentfilter command.allDepartments; department>
				<input
					type="checkbox"
					name="${status.expression}"
					value="${department.code}"
					data-related-filter="modules"
					data-short-value="${department.name}"
					${contains_by_code(command.departments, department)?string('checked','')}
				>
				${department.name}
			</@filter>

			<#-- Module filter-->
			<#assign placeholder = "All modules" />
			<#assign currentfilter><@current_filter_value "command.modules" placeholder; module>${module.code}</@current_filter_value></#assign>
			<#assign moduleCustomPicker>
				<div class="module-search input-append input-group">
					<input class="module-search-query module-picker module prevent-reload form-control" type="text" value="" placeholder="Search for a module" />
					<span class="add-on input-group-addon"><i class="fa fa-search"></i></span>
				</div>
			</#assign>
			<@filter name="modules" path="command.modules" placeholder=placeholder currentFilter=currentfilter allItems=command.allModules customPicker=moduleCustomPicker; module>
				<input
					type="checkbox"
					name="${status.expression}"
					value="${module.code}"
					data-related-value="${module.adminDepartment.code}"
					data-short-value="${module.code?upper_case}"
					${contains_by_code(command.modules, module)?string('checked','')}
				>
				${module.code?upper_case} ${module.name}
			</@filter>

			<#-- State filter-->
			<#assign placeholder = "All states" />
			<#assign currentfilter><@current_filter_value "command.states" placeholder; state>${state.description}</@current_filter_value></#assign>
			<@filter "states" "command.states" placeholder currentfilter command.allStates; state>
				<input
					type="checkbox"
					name="${status.expression}"
					value="${state.dbValue}"
					data-short-value="${state.description}"
					${contains_by_db_value(command.states, state)?string('checked','')}
				>
				${state.description}
			</@filter>

			<#-- Time filter-->
			<#assign placeholder = "All dates" />
			<#assign currentfilter><@current_filter_value "command.times" placeholder; time>${time.code}</@current_filter_value></#assign>
			<@filter "times" "command.times" placeholder currentfilter command.allTimes; time>
				<input
					type="checkbox"
					name="${status.expression}"
					value="${time.code}"
					data-short-value="${time.code}"
					${contains_by_code(command.times, time)?string('checked','')}
				>
				${time.code}
			</@filter>

			<button type="button" class="clear-all-filters btn btn-sm btn-filter">
				Clear filters
			</button>
		</@f.form>
	</div>

  <div class="col-md-12">
		<script language="JavaScript">

		</script>
		<ul class="pagination pagination-sm pull-right">
  		<#if currentPage lte 1>
  			<li class="disabled"><span>&laquo;</span></li>
			<#else>
  			<li><a href="?page=${currentPage - 1}">&laquo;</a></li>
			</#if>

  		<#list 1..totalPages as page>
				<#if page == currentPage>
  				<li class="active"><span>${page}</span></li>
				<#else>
  				<li><a href="?page=${page}">${page}</a></li>
				</#if>
			</#list>

  		<#if currentPage gte totalPages>
  			<li class="disabled"><span>&raquo;</span></li>
			<#else>
  			<li><a href="?page=${currentPage + 1}">&raquo;</a></li>
			</#if>
		</ul>
	</div>

	<div class="filter-results">
		<#include "_filter_results.ftl" />
	</div>

</#compress></#escape>
