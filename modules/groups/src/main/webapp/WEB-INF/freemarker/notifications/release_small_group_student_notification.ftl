You have been allocated the following small teaching groups:

${group.groupSet.name} ${group.groupSet.format.description} for ${group.groupSet.module.code} - ${group.groupSet.module.name}
${group.name} - <@fmt.p group.students.members?size "student"/>

<#list group.events as event><#if !event.unscheduled>
${event.startTime} ${event.day.name}, ${event.location!}, <@fmt.weekRanges event />
</#if></#list>

Please visit  <@url page=profileUrl context="/" /> to view these groups.