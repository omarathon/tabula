<#escape x as x?html>

<h1>View and record monitoring points</h1>

<p><em>Choose the academic year to view and record:</em></p>
<ul class="links">
	<li>
		<h3><a id="view-department-${department.code}" href="<@routes.attendance.viewHomeForYear department '2013'/>">${department.name} 13/14</a></h3>
		<#if features.academicYear2014>
			<h3><a id="view-department-${department.code}" href="<@routes.attendance.viewHomeForYear department '2014'/>">${department.name} 14/15</a></h3>
		</#if>
		<#if features.academicYear2015>
			<h3><a id="view-department-${department.code}" href="<@routes.attendance.viewHomeForYear department '2015'/>">${department.name} 15/16</a></h3>
		</#if>
		<#if features.academicYear2016>
			<h3><a id="view-department-${department.code}" href="<@routes.attendance.viewHomeForYear department '2016'/>">${department.name} 16/17</a></h3>
		</#if>
	</li>
</ul>

</#escape>