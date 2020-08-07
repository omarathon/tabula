<#list batches as batch>

${batch.timeStatement}:

<#list batch.items as item>
* Feedback for ${item.assignment.module.code?upper_case} ${item.assignment.module.name} ${item.assignment.name} is due<#if item.dueToday> today<#elseif item.deadline??> in <@fmt.p item.daysLeft "working day" /> on ${item.dateOnlyFormatter.print(item.deadline)}</#if>.
</#list></#list>
