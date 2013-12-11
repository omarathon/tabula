<#escape x as x?html>

<#import "../attendance_macros.ftl" as attendance_macros />
<#import "../attendance_variables.ftl" as attendance_variables />

<#assign thisPath><@routes.agentView command.relationshipType /></#assign>

<h1>My ${command.relationshipType.studentRole}s</h1>

<#if students?size == 0>
	<p><em>No ${command.relationshipType.studentRole}s were found.</em></p>
<#else>

	<#function view_url student>
		<#local return>
			<@routes.agentStudentView student command.relationshipType command.academicYear />
		</#local>
		<#return return/>
	</#function>

	<#function record_url student>
		<#local return>
			<@routes.agentStudentRecord student command.relationshipType command.academicYear thisPath />
		</#local>
		<#return return/>
	</#function>

	<#include "../home/_points_table_js_sort.ftl" />

</#if>
</#escape>