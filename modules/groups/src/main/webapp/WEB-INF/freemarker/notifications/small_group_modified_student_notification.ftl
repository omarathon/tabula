Your small teaching group allocation for ${group.groupSet.module.code?upper_case} - "${group.groupSet.module.name}" has been updated

${group.groupSet.name} ${group.groupSet.format.description} for ${group.groupSet.module.code?upper_case} - ${group.groupSet.module.name}
${group.name} - <@fmt.p number=group.students.members?size singular="student"/>

<#list group.events as event>
<@fmt.time time=event.startTime /> ${event.day.name}, ${event.location}, <@fmt.weekRanges event />
</#list>


Please visit  <@url page=profileUrl context="/profiles" /> to view this group.
