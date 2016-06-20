<#escape x as x?html>

<h1>My ${relationshipType.studentRole}s</h1>

<p><em>Choose the academic year to view and record:</em></p>
<ul class="links">
	<li><h3><a id="relationship-${relationshipType.urlPart}" href="<@routes.attendance.agentHomeForYear relationshipType '2013'/>">${relationshipType.studentRole?cap_first}s 13/14</a></h3></li>
	<#if features.attendanceMonitoringAcademicYear2014>
		<li><h3><a id="relationship-${relationshipType.urlPart}-2014" href="<@routes.attendance.agentHomeForYear relationshipType '2014'/>">${relationshipType.studentRole?cap_first}s 14/15</a></h3></li>
	</#if>
	<#if features.attendanceMonitoringAcademicYear2015>
		<li><h3><a id="relationship-${relationshipType.urlPart}-2015" href="<@routes.attendance.agentHomeForYear relationshipType '2015'/>">${relationshipType.studentRole?cap_first}s 15/16</a></h3></li>
	</#if>
</ul>

</#escape>