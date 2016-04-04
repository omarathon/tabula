<#escape x as x?html>
	
<#macro categoryRow categories columns showSectionLabels=true>
	<#local currentSection = "" />
	<#local currentCategory = "" />
	<#list columns as column>
		<#if showSectionLabels && column.sectionIdentifier?has_content && currentSection != column.sectionIdentifier>
			<#local currentSection = column.sectionIdentifier />
			<td class="borderless"></td>
		</#if>
		<#if column.category?has_content>
			<#if currentCategory != column.category>
				<#local currentCategory = column.category />
				<th class="rotated first-in-category" colspan="${categories[column.category]?size}"><div class="rotate">${column.category}</div></th>
			</#if>
		<#else>
			<td class="borderless"></td>
		</#if>
	</#list>
</#macro>

<#macro titleInCategoryRow categories columns showSectionLabels=true>
	<#local currentSection = "" />
	<#local currentCategory = "" />
	<#list columns as column>
		<#if showSectionLabels && column.sectionTitleLabel?has_content>
			<#if currentSection != column.sectionIdentifier>
				<#local currentSection = column.sectionIdentifier />
				<th class="rotated first-in-category"><div class="rotate middle">${column.sectionTitleLabel}</div></th>
			</#if>
		</#if>
		<#if column.category?has_content>
			<#if currentCategory != column.category>
				<#local firstInCategory = true />
				<#local currentCategory = column.category />
			<#else>
				<#local firstInCategory = false />
			</#if>
			<td class="rotated <#if firstInCategory!false>first-in-category</#if>"><div class="rotate" title="${column.title}">${column.title}</div></td>
		<#else>
			<td class="borderless"></td>
		</#if>
	</#list>
</#macro>

<#macro headerRow columns showSectionLabels=true>
	<#local currentSection = "" />
	<#local currentCategory = "" />
	<#list columns as column>
		<#if showSectionLabels && column.sectionSecondaryValueLabel?has_content && currentSection != column.sectionIdentifier>
			<#local currentSection = column.sectionIdentifier />
			<th class="section-secondary-label rotated first-in-category"><div class="rotate middle nomargin">${column.sectionSecondaryValueLabel}</div></th>
		</#if>
		<#if column.category?has_content && currentCategory != column.category>
			<#local firstInCategory = true />
			<#local currentCategory = column.category />
		<#else>
			<#local firstInCategory = false />
		</#if>
		<#if !column.category?has_content>
			<th>${column.title}</th>
		<#elseif column.renderSecondaryValue?has_content>
			<td <#if firstInCategory!false>class="first-in-category"</#if>><#noescape>${column.renderSecondaryValue}</#noescape></td>
		<#else>
			<td <#if firstInCategory!false>class="first-in-category"</#if>></td>
		</#if>
	</#list>
</#macro>

<#macro entityRows entity isFirstEntity entityCount columns columnValues showSectionLabels=true>
	<#local currentSection = "" />
	<#local currentCategory = "" />
	<#list columnValues as columnValue>
		<#local column = columns[columnValue_index] />
		<#if showSectionLabels && column.sectionValueLabel?has_content && currentSection != column.sectionIdentifier && isFirstEntity>
			<#local currentSection = column.sectionIdentifier />
			<th rowspan="${entityCount}" class="section-value-label rotated first-in-category"><div class="rotate middle">${column.sectionValueLabel}</div></th>
		</#if>
		<#if column.category?has_content && currentCategory != column.category>
			<#local firstInCategory = true />
			<#local currentCategory = column.category />
		<#else>
			<#local firstInCategory = false />
		</#if>
		<td <#if firstInCategory!false>class="first-in-category"</#if>>
			<#if columnValue[entity.id]?has_content><#noescape>${columnValue[entity.id]}</#noescape></#if>
		</td>
	</#list>
</#macro>

</#escape>