<#escape x as x?html>

<#macro deptheaderroutemacro dept>
	<@routes.viewHomeForYear dept academicYear.startYear?c />
</#macro>
<#assign deptheaderroute = deptheaderroutemacro in routes/>
<@fmt.deptheader "View and record attendance for ${academicYear.toString}" "in" department routes "deptheaderroute" />

<#if hasSchemes>
	<h3><a href="<@routes.viewStudents department academicYear.startYear?c />">View by student and report to SITS:eVision</a></h3>
	<h3><a href="<@routes.viewPoints department academicYear.startYear?c />">View by point</a></h3>
	<#if can.do("MonitoringPoints.View", department)>
		<#list department.displayedStudentRelationshipTypes as relationshipType>
		<h3><a href="<@routes.viewAgents department academicYear.startYear?c relationshipType />">View by ${relationshipType.agentRole}</a></h3>
		</#list>
	</#if>
<#else>
	<p class="alert alert-information"><i class="icon-info-sign"></i> There are no monitoring point schemes for this department for this academic year.</p>
</#if>

</#escape>