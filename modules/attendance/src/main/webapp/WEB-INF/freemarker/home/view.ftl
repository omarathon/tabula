<#escape x as x?html>

<h2>View and record attendance for ${department.name}</h2>

<div class="btn-toolbar dept-toolbar">
	<#if department.parent??>
		<a class="btn btn-medium use-tooltip" href="<@routes.viewDepartmentPoints department.parent />" data-container="body" title="${department.parent.name}">
			Parent department
		</a>
	</#if>

	<#if department.children?has_content>
		<div class="btn-group">
			<a class="btn btn-medium dropdown-toggle" data-toggle="dropdown" href="#">
				Subdepartments
				<span class="caret"></span>
			</a>
			<ul class="dropdown-menu pull-right">
				<#list department.children as child>
					<li><a href="<@routes.viewDepartmentPoints child />">${child.name}</a></li>
				</#list>
			</ul>
		</div>
	</#if>
</div>

<#if hasSets>
	<h3><a href="<@routes.viewDepartmentStudents department />">View by student and report to SITS:eVision</a></h3>
	<h3><a href="<@routes.viewDepartmentPoints department />">View by point</a></h3>
	<#if can.do("MonitoringPoints.View", department)>
		<#list department.displayedStudentRelationshipTypes as relationshipType>
		<h3><a href="<@routes.viewDepartmentAgents department relationshipType/>">View by ${relationshipType.agentRole}</a></h3>
		</#list>
	</#if>
<#else>
	<p><em>There are no monitoring point schemes for this department</em></p>
</#if>
</#escape>