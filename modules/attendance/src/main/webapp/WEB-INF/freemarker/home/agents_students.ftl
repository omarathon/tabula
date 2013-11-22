<#escape x as x?html>

<#import "../attendance_macros.ftl" as attendance_macros />
<#import "../attendance_variables.ftl" as attendance_variables />

<#assign thisPath><@routes.viewDepartmentAgentsStudents command.department command.relationshipType command.agent /></#assign>

<h1>${command.agent.fullName}'s ${command.relationshipType.studentRole}s</h1>

<#if students?size == 0>
	<p><em>No ${command.relationshipType.studentRole}s were found.</em></p>
<#else>

	<#function view_url student>
		<#local return>
			<@routes.viewStudent command.department student command.academicYear />
		</#local>
		<#return return/>
	</#function>

	<#function record_url student>
		<#local return>
			<@routes.recordStudent command.department student command.academicYear thisPath />
		</#local>
		<#return return/>
	</#function>

	<#include "_points_table_js_sort.ftl" />

</#if>
</#escape>