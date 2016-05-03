<#escape x as x?html>

<#function route_function dept>
	<#local result><@routes.routes.department_timetables dept /></#local>
	<#return result />
</#function>
<@fmt.id7_deptheader "Timetables" route_function "for" />

<#assign submitUrl><@routes.profiles.department_timetables department /></#assign>
<#include "_department_timetable.ftl" />
</#escape>