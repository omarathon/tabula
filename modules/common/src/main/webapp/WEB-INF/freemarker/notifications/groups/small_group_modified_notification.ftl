Your small teaching group allocation for ${groupSet.module.code?upper_case} - "${groupSet.module.name?trim}" has been updated

${groupSet.name} ${groupSet.format.description} for ${groupSet.module.code?upper_case} - ${groupSet.module.name}

<#list groups as group>
${group.name} - <@fmt.p number=group.students.members?size singular="student"/>
<#list group.events as event><#if !event.unscheduled>
   <@fmt.time time=event.startTime /> ${event.day.name}, ${(event.location.name)!}, <@fmt.weekRanges event />
</#if></#list>

</#list>