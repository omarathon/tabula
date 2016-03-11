<#escape x as x?html>
	<#macro deptheaderroutemacro dept>
		<@routes.profiles.draft_department_timetables dept academicYear endpoint />
	</#macro>
	<#assign deptheaderroute = deptheaderroutemacro in routes/>
	<@fmt.deptheader "Draft timetable" "for" department routes "deptheaderroute" />

	<#assign submitUrl><@routes.profiles.draft_department_timetables department academicYear endpoint /></#assign>
	<#include "_department_timetable.ftl" />
</#escape>