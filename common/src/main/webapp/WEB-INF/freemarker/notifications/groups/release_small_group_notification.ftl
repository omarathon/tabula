You have been allocated the following small teaching groups:

<#list groups as group>
- ${group.groupSet.module.code?upper_case} ${group.groupSet.nameWithoutModulePrefix} ${group.name}, <@fmt.p number=group.students.members?size singular="student"/>
<#list group.events as event>
    - <#if !event.unscheduled><@fmt.time time=event.startTime /> ${event.day.name}, <#if (event.location.name)!?length gt 0>${(event.location.name)!}, </#if><@fmt.weekRanges object=event/><#else>(unscheduled event)</#if><#if event.tutors.users?size gt 0>, <@fmt.p number=event.tutors.users?size singular="Tutor" shownumber=false/>:<#list event.tutors.users as tutor> ${tutor.fullName}<#if tutor_has_next>,</#if></#list></#if>
</#list>
</#list>
