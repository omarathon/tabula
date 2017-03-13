<#escape x as x?html>

<#function route_function dept>
	<#local result><@routes.routes.department_timetables dept /></#local>
	<#return result />
</#function>
<@fmt.id7_deptheader "Timetables" route_function "for" />

<#assign submitUrl><@routes.profiles.department_timetables department /></#assign>
<#assign calendarDownloadUrl><@routes.profiles.department_timetables_calendar_download department /></#assign>
<#function timetableDownloadRoute dept year>
	<#local result><@routes.profiles.department_timetables_download dept year /></#local>
	<#return result />
</#function>
<#include "_department_timetable.ftl" />
</#escape>