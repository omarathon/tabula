<#escape x as x?html>

<h1>Manage monitoring points</h1>

<#if managePermissions?size == 0>
	<p><em>You do not have permission to manage the monitoring points for any department.</em></p>
<#else>
	<p><em>Choose the department to manage:</em></p>
	<ul class="links">
		<#list managePermissions as department>
			<li>
				<h3><a id="manage-department-${department.code}" href="<@routes.attendance.manageHomeForYear department '2013'/>">${department.name} 13/14</a></h3>
				<#if features.academicYear2014>
					<h3><a id="manage-department-${department.code}" href="<@routes.attendance.manageHomeForYear department '2014'/>">${department.name} 14/15</a></h3>
				</#if>
				<#if features.academicYear2015>
					<h3><a id="manage-department-${department.code}" href="<@routes.attendance.manageHomeForYear department '2015'/>">${department.name} 15/16</a></h3>
				</#if>
				<#if features.academicYear2016>
					<h3><a id="manage-department-${department.code}" href="<@routes.attendance.manageHomeForYear department '2016'/>">${department.name} 16/17</a></h3>
				</#if>
			</li>
		</#list>
	</ul>
</#if>

</#escape>