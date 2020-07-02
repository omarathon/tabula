<#macro gradeOptions grades default={} currentGrade=''>
  <#local seen = false />
  <option value="" <#if !default??>selected</#if>></option>
  <#list grades as grade>
    <#if grade.grade == currentGrade><#local seen = true /></#if>
    <option value="${grade.grade}" <#if (currentGrade?has_content && grade.grade == currentGrade) || (!(currentGrade?has_content) && default?? && grade.grade == default.grade!)>selected</#if>>${grade.grade}</option>
  </#list>
  <#if !seen && currentGrade?has_content><option value="${currentGrade}" selected>${currentGrade}</option></#if>
</#macro>
