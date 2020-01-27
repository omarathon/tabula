<#list batches as batch>

${batch.timeStatement}:

<#list batch.items as item>
* '${item.assignment.name}' for ${item.module.code?upper_case} ${item.module.name}. Your deadline for this assignment ${item.deadlineDate}.<#if item.cantSubmit> You can no longer submit this assignment via Tabula.</#if>
</#list></#list>
