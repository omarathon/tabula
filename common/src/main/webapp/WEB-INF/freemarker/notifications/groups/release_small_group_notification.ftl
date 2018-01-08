You have been allocated the following small teaching groups:

<#list groups as group>
- ${group.groupSet.module.code?upper_case} ${group.groupSet.nameWithoutModulePrefix} ${group.name}
  <@fmt.p number=group.students.members?size singular="student"/>

<#list group.events as event>
	<@fmt.p number=event.tutors.users?size singular="Tutor" shownumber=false/>:<#list event.tutors.users as tutor> ${tutor.fullName}</#list>
	<#if !event.unscheduled>
	<@fmt.time time=event.startTime /> ${event.day.name}, <#if (event.location.name)!?length gt 0>${(event.location.name)!}, </#if><@fmt.weekRanges object=event stripHtml=true />
	</#if>
</#list>
</#list>