<#escape x as x?html>

<@fmt.deptheader "View and record attendance" "for" department routes "viewDepartment" />

<#if hasSets>
	<h3><a href="<@routes.viewDepartmentStudents department />">View by student and report to SITS:eVision</a></h3>
	<h3><a href="<@routes.viewDepartmentPoints department />">View by point</a></h3>
	<#if can.do("MonitoringPoints.View", department)>
		<#list department.displayedStudentRelationshipTypes as relationshipType>
		<h3><a href="<@routes.viewDepartmentAgents department relationshipType/>">View by ${relationshipType.agentRole}</a></h3>
		</#list>
	</#if>
<#else>
	<p class="alert alert-information"><i class="icon-info-sign"></i> There are no monitoring point schemes for this department.</p>
</#if>
</#escape>