<#escape x as x?html>

<#macro deptheaderroutemacro dept>
	<@routes.attendance.viewAgentsHome dept academicYear.startYear?c />
</#macro>
<#assign deptheaderroute = deptheaderroutemacro in routes/>
<@fmt.deptheader "View and record attendance for ${academicYear.toString}" "in" department routes "deptheaderroute" />

<#if can.do("MonitoringPoints.View", department)>
	<#list department.displayedStudentRelationshipTypes as relationshipType>
	<h3><a href="<@routes.attendance.viewAgents department academicYear.startYear?c relationshipType />">View by ${relationshipType.agentRole}</a></h3>
	</#list>
</#if>

</#escape>