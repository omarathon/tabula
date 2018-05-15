<#escape x as x?html>

<#macro showMarksShort entity markType>
	<#list perYearColumns?keys?sort as year>
		<#if gridOptionsCommand.showComponentMarks>
		<th><span class="use-tooltip" title="${markType.description}">${markType.label}</span></th>
		</#if>

		<#assign colsUsed = 0 />
		<#list mapGet(perYearModuleMarkColumns, year) as column>
			<#assign hasValue = !column.isEmpty(entity, year) />
			<#if hasValue && mapGet(perYearColumnValues, column)?? && mapGet(mapGet(perYearColumnValues, column), entity)?? && mapGet(mapGet(mapGet(perYearColumnValues, column), entity), year)??>
			<td>
					<#assign values = mapGet(mapGet(mapGet(mapGet(perYearColumnValues, column), entity), year), markType) />
					<#list values as value><#noescape>${value.toHTML}</#noescape><#if value_has_next>,</#if></#list>
			</td>
				<#assign colsUsed = colsUsed + 1>
			</#if>
		</#list>

		<#assign reportCols = mapGet(perYearModuleReportColumns, year) />
		<#assign yearPadding = mapGet(maxYearColumnSize, year) - colsUsed>
		<#list 0..<yearPadding as i><td>&nbsp;</td></#list>

		<#list reportCols as column>
		<td>
			<#if mapGet(perYearColumnValues, column)?? && mapGet(mapGet(perYearColumnValues, column), entity)?? && mapGet(mapGet(mapGet(perYearColumnValues, column), entity), year)??>
				<#assign values = mapGet(mapGet(mapGet(mapGet(perYearColumnValues, column), entity), year), markType) />
				<#list values as value><#noescape>${value.toHTML}</#noescape><#if value_has_next>,</#if></#list>
			</#if>
		</td>
		</#list>
	</#list>
</#macro>

<table class="table table-condensed grid <#if !gridOptionsCommand.showComponentMarks>with-hover</#if>">
	<#-- Category row -->
	<tr class="category">
		<#list studentInformationColumns as column><td class="borderless">&nbsp;</td></#list>
		<#list perYearColumns?keys?sort as year>
			<td class="spacer">&nbsp;</td>
			<td class="spacer" colspan="${mapGet(maxYearColumnSize, year)}">&nbsp;</td>
			<#list mapGet(perYearModuleReportColumns, year) as column><td class="spacer">&nbsp;</td></#list>
			<#if !year_has_next><td class="spacer">&nbsp;</td></#if>
		</#list>
		<#assign currentCategory = '' />
		<#list summaryColumns as column>
			<#if column.category?has_content>
				<#if currentCategory != column.category>
					<#assign currentCategory = column.category />
					<th class="rotated" colspan="${chosenYearColumnCategories[column.category]?size}"><div class="rotate-outer"><div class="rotate">${column.category}</div></div></th>
				</#if>
			<#else>
				<td>&nbsp;</td>
			</#if>
		</#list>
	</tr>
	<#-- Header row -->
	<tr class="header">
		<#list studentInformationColumns as column>
			<th>${column.title}</th>
		</#list>

		<#list perYearColumns?keys?sort as year>
			<td class="spacer">&nbsp;</td>

			<#assign yearSize = mapGet(maxYearColumnSize, year)>
			<th colspan="${yearSize}">Year ${year}</th>

			<#list mapGet(perYearModuleReportColumns, year) as column>
				<th class="rotated <#if column.category?has_content>has-category</#if>"><div class="rotate-outer"><div class="rotate">${column.title}</div></div></th>
			</#list>

			<#if !year_has_next><td class="spacer">&nbsp;</td></#if>
		</#list>

		<#list summaryColumns as column>
			<th class="rotated <#if column.boldTitle>bold</#if> <#if column.category?has_content>has-category</#if>"><div class="rotate-outer"><div class="rotate">${column.title}</div></div></th>
		</#list>
	</tr>

		<#-- Entities -->
		<#list entities as entity>
			<tbody>
				<tr class="student <#if entity_index%2 == 1>odd</#if>">
					<#list studentInformationColumns as column>
						<td rowspan="<#if gridOptionsCommand.showComponentMarks>4<#else>2</#if>">
							<#assign hasValue = mapGet(chosenYearColumnValues, column)?? && mapGet(mapGet(chosenYearColumnValues, column), entity)?? />
							<#if hasValue>
								<#noescape>${mapGet(mapGet(chosenYearColumnValues, column), entity).toHTML}</#noescape>
							</#if>
						</td>
					</#list>

					<#list perYearColumns?keys?sort as year>
						<td rowspan="<#if !gridOptionsCommand.showComponentMarks>2</#if>" class="spacer">&nbsp;</td>
						<#assign colsUsed = 0 />
						<#list mapGet(perYearModuleMarkColumns, year) as column>
							<#assign hasValue = !column.isEmpty(entity, year) />
							<#if hasValue>
								<td class="rotated">
									<div class="rotate-outer">
										<div class="rotate">${column.title} - ${column.secondaryValue} <i>${column.categoryShortForm!""}</i></div>
									</div>
								</td>
								<#assign colsUsed = colsUsed + 1>
							</#if>
						</#list>

						<#assign reportCols = mapGet(perYearModuleReportColumns, year) />
						<#assign yearPadding = mapGet(maxYearColumnSize, year) + reportCols?size - colsUsed>
						<#list 0..<yearPadding as i><td>&nbsp;</td></#list>
						<#if !year_has_next><td rowspan="<#if gridOptionsCommand.showComponentMarks>4<#else>2</#if>" class="spacer">&nbsp;</td></#if>
					</#list>

					<#list summaryColumns as column>
						<td rowspan="<#if gridOptionsCommand.showComponentMarks>4<#else>2</#if>">
							<#assign hasValue = mapGet(chosenYearColumnValues, column)?? && mapGet(mapGet(chosenYearColumnValues, column), entity)?? />
							<#if hasValue>
								<#noescape>${mapGet(mapGet(chosenYearColumnValues, column), entity).toHTML}</#noescape>
							</#if>
						</td>
					</#list>
				</tr>

				<tr class="overall assignments <#if entity_index%2 == 1>odd</#if>">
					<@showMarksShort entity ExamGridColumnValueType.Overall />
				</tr>
				<#if gridOptionsCommand.showComponentMarks>
					<tr class="assignments <#if entity_index%2 == 1>odd</#if>">
						<@showMarksShort entity ExamGridColumnValueType.Assignment />
					</tr>
					<tr class="exams <#if entity_index%2 == 1>odd</#if>">
						<@showMarksShort entity ExamGridColumnValueType.Exam />
					</tr>
				</#if>
			</tbody>
		</#list>
</table>
</#escape>