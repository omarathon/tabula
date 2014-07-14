<#escape x as x?html>

<h1>My students</h1>

<ul>
	<#list relationshipTypesMap?keys as relationshipType>
		<#if mapGet(relationshipTypesMap, relationshipType)>
			<li><h3><a id="relationship-${relationshipType.urlPart}" href="<@routes.agentView relationshipType />">${relationshipType.studentRole?cap_first}s 13/14</a></h3></li>
			<#if features.attendanceMonitoringAcademicYear2014>
				<li><h3><a id="relationship-${relationshipType.urlPart}-2014" href="<@routes.agentHomeForYear relationshipType '2014'/>">${relationshipType.studentRole?cap_first}s 14/15</a></h3></li>
			</#if>
		</#if>
	</#list>
</ul>

</#escape>