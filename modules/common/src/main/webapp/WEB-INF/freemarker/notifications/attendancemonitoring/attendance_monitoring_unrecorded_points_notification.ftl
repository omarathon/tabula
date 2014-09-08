${points?size} monitoring points in ${department.name} need attendance recording:

<#list points as point>
${point.name} (<#if point.scheme.pointStyle.dbValue == 'week'>${wholeWeekFormatter(point.startWeek, point.endWeek, academicYear, department, false)?replace('<sup>','')?replace('</sup>','')}<#else>${intervalFormatter(point.startDate, point.endDate)}</#if>)
</#list>