<#escape x as x?html>
<@fmt.deptheader "Timetables" "for" department routes.profiles "department_timetables" />

<#assign submitUrl><@routes.profiles.department_timetables department /></#assign>
<#include "_department_timetable.ftl" />
</#escape>