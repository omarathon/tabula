<#escape x as x?html>

<h1>View and record monitoring points</h1>

<#if viewPermissions?size == 0>
	<p><em>You do not have permission to view the monitoring points for any department.</em></p>
<#else>
	<p><em>Choose the department to view and record:</em></p>
	<ul class="links">
		<#list viewPermissions as department>
			<li>
				<h3><a id="view-department-${department.code}" href="<@routes.attendance.viewHomeForYear department '2013'/>">${department.name} 13/14</a></h3>
				<#if features.attendanceMonitoringAcademicYear2014>
					<h3><a id="view-department-${department.code}" href="<@routes.attendance.viewHomeForYear department '2014'/>">${department.name} 14/15</a></h3>
				</#if>
				<#if features.attendanceMonitoringAcademicYear2015>
					<h3><a id="view-department-${department.code}" href="<@routes.attendance.viewHomeForYear department '2015'/>">${department.name} 15/16</a></h3>
				</#if>
			</li>
		</#list>
	</ul>
</#if>

</#escape>