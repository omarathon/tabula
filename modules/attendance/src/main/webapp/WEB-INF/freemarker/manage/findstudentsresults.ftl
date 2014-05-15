<#function sortClass field>
	<#list command.sortOrder as order>
		<#if order.propertyName == field>
			<#if order.ascending>
				<#return "headerSortDown" />
			<#else>
				<#return "headerSortUp" />
			</#if>
		</#if>
	</#list>
	<#return "" />
</#function>

<#macro pagination currentPage totalResults resultsPerPage extra_classes="">
	<#local totalPages = (totalResults / resultsPerPage)?ceiling />
	<div class="pagination pagination-right ${extra_classes}">
		<ul>
			<#if currentPage lte 1>
				<li class="disabled"><span>&laquo;</span></li>
			<#else>
				<li><a href="?page=${currentPage - 1}" data-page="${currentPage - 1}">&laquo;</a></li>
			</#if>

			<#list 1..totalPages as page>
				<#if page == currentPage>
					<li class="active"><span>${page}</span></li>
				<#else>
					<li><a href="?page=${page}" data-page="${page}">${page}</a></li>
				</#if>
			</#list>

			<#if currentPage gte totalPages>
				<li class="disabled"><span>&raquo;</span></li>
			<#else>
				<li><a href="?page=${currentPage + 1}" data-page="${currentPage + 1}">&raquo;</a></li>
			</#if>
		</ul>
	</div>
</#macro>

<#if (totalResults == 0)>
	<p><em>No students found.</em></p>
<#else>
	<#include "studentstable.ftl" />
</#if>