<#escape x as x?html>

<@fmt.deptheader "View and record attendance for ${academicYear.toString}" "in" department routes "viewDepartment" />

<#if can.do("MonitoringPoints.View", department)>
	<#list department.displayedStudentRelationshipTypes as relationshipType>
	<h3><a href="<@routes.viewAgents department academicYear.startYear?c relationshipType />">View by ${relationshipType.agentRole}</a></h3>
	</#list>
</#if>

</#escape>