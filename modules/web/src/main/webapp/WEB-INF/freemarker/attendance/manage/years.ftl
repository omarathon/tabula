<#escape x as x?html>

<h1>Manage monitoring points</h1>

	<p><em>Choose the academic year to manage:</em></p>
	<ul class="links">
		<li>
			<h3><a id="manage-department-${department.code}" href="<@routes.attendance.manageHomeForYear department '2013'/>">${department.name} 13/14</a></h3>
			<#if features.attendanceMonitoringAcademicYear2014>
				<h3><a id="manage-department-${department.code}" href="<@routes.attendance.manageHomeForYear department '2014'/>">${department.name} 14/15</a></h3>
			</#if>
			<#if features.attendanceMonitoringAcademicYear2015>
				<h3><a id="manage-department-${department.code}" href="<@routes.attendance.manageHomeForYear department '2015'/>">${department.name} 15/16</a></h3>
			</#if>
		</li>
	</ul>

</#escape>