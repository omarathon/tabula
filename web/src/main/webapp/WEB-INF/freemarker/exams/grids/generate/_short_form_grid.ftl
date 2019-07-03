<#escape x as x?html>
  <#include "*/exams_macros.ftl" />
  <#assign years = perYearColumns?keys?sort />
  <#macro showMarksShort entity markType>
    <#list years as year>
      <#if gridOptionsCommand.showComponentMarks>
        <th><span class="tabula-tooltip" data-title="${markType.description}">${markType.label}</span></th>
      </#if>

      <#assign colsUsed = 0 />
      <#list mapGet(perYearModuleMarkColumns, year) as column>
        <#assign columnValue = "" />
        <#if !column.isEmpty(entity, year)>
          <#assign columnValue = generateColumnValue(perYearColumnValues, entity, year, column) />
        </#if>
        <#if columnValue?has_content>
          <td>
            <#assign values = mapGet(columnValue, markType) />
            <#list values as value><#noescape>${value.toHTML}</#noescape><#if value_has_next>,</#if></#list>
          </td>
          <#assign colsUsed = colsUsed + 1>
        </#if>
      </#list>

      <#assign reportCols = mapGet(perYearModuleReportColumns, year) />
      <#assign yearPadding = mapGet(maxYearColumnSize, year) - colsUsed>
      <#list 0..<yearPadding as i>
        <td>&nbsp;</td></#list>

      <#list reportCols as column>
        <td>
          <#assign columnValue = generateColumnValue(perYearColumnValues, entity, year, column) />
          <#if columnValue?has_content>
            <#assign values = mapGet(columnValue, markType) />
            <#list values as value><#noescape>${value.toHTML}</#noescape><#if value_has_next>,</#if></#list>
          </#if>
        </td>
      </#list>
    </#list>
  </#macro>

  <div class="id7-wide-table-wrapper-container no-wide-tables">
    <div class="table-responsive">
      <table class="table table-condensed grid <#if !gridOptionsCommand.showComponentMarks>with-hover</#if>">
        <thead>
        <#-- Category row -->
        <tr class="category">
          <#list studentInformationColumns as column>
            <td class="borderless">&nbsp;</td></#list>
          <#list years as year>
            <td class="spacer">&nbsp;</td>
            <td class="spacer" colspan="${mapGet(maxYearColumnSize, year)}">&nbsp;</td>
            <#list mapGet(perYearModuleReportColumns, year) as column>
              <td class="spacer">&nbsp;</td></#list>
            <#if !year_has_next>
              <td class="spacer">&nbsp;</td></#if>
          </#list>
          <#assign currentCategory = '' />
          <#list summaryColumns as column>
            <#if column.category?has_content>
              <#if currentCategory != column.category>
                <#assign currentCategory = column.category />
                <th class="rotated" colspan="${chosenYearColumnCategories[column.category]?size}"><span class="rotate">${column.category}</span></th>
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

          <#list years as year>
            <td class="spacer">&nbsp;</td>

            <#assign yearSize = mapGet(maxYearColumnSize, year)>
            <th colspan="${yearSize}">Year ${year}</th>

            <#list mapGet(perYearModuleReportColumns, year) as column>
              <th class="rotated <#if column.category?has_content>has-category</#if>"><span class="rotate">${column.title}</span></th>
            </#list>

            <#if !year_has_next>
              <td class="spacer">&nbsp;</td></#if>
          </#list>

          <#list summaryColumns as column>
            <th class="rotated <#if column.boldTitle>bold</#if> <#if column.category?has_content>has-category</#if>"><span class="rotate">${column.title}</span>
            </th>
          </#list>
        </tr>
        </thead>

        <#-- Entities -->
        <#list entities as entity>
          <tbody>
          <tr class="student <#if entity_index%2 == 1>odd</#if>">
            <#list studentInformationColumns as column>
              <td rowspan="<#if gridOptionsCommand.showComponentMarks>4<#else>2</#if>">
                <#if mapGet(chosenYearColumnValues, column)??>
                  <#assign columnValue = mapGet(mapGet(chosenYearColumnValues, column), entity) />
                <#else>
                  <#assign columnValue = ""/>
                </#if>
                <#if columnValue?has_content><#noescape>${columnValue.toHTML}</#noescape></#if>
              </td>
            </#list>

            <#list years as year>
              <td rowspan="<#if !gridOptionsCommand.showComponentMarks>2</#if>" class="spacer">&nbsp;</td>
              <#assign colsUsed = 0 />
              <#list mapGet(perYearModuleMarkColumns, year) as column>
                <#assign hasValue = !column.isEmpty(entity, year) />
                <#if hasValue>
                  <td class="rotated">
                    <span class="rotate">${column.title} - ${column.secondaryValue} <i>${column.categoryShortForm!""}</i></span>
                  </td>
                  <#assign colsUsed = colsUsed + 1>
                </#if>
              </#list>

              <#assign reportCols = mapGet(perYearModuleReportColumns, year) />
              <#assign yearPadding = mapGet(maxYearColumnSize, year) + reportCols?size - colsUsed>
              <#list 0..<yearPadding as i>
                <td>&nbsp;</td></#list>
              <#if !year_has_next>
                <td rowspan="<#if gridOptionsCommand.showComponentMarks>4<#else>2</#if>" class="spacer">&nbsp;</td></#if>
            </#list>

            <#list summaryColumns as column>
              <td rowspan="<#if gridOptionsCommand.showComponentMarks>4<#else>2</#if>">
                <#if mapGet(chosenYearColumnValues, column)??>
                  <#assign columnValue = mapGet(mapGet(chosenYearColumnValues, column), entity)!"" />
                <#else>
                  <#assign columnValue = ""/>
                </#if>
                <#if columnValue?has_content><#noescape>${columnValue.toHTML}</#noescape></#if>
                <#if !column_has_next>
                  <script nonce="${nonce()}">
                    jQuery('#examGridSpinner').find('.progress-bar')
                      .attr('aria-valuenow', '${entity_index+1}')
                      .css('width', '${((entity_index+1)/entities?size)*100}%');
                  </script></#if>
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
    </div>
  </div>
</#escape>