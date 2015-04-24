<#escape x as x?html>

<#macro deptheaderroutemacro dept>
	<@routes.reports.departmentWithYear dept academicYear />
</#macro>
<#assign deptheaderroute = deptheaderroutemacro in routes.reports />
<@fmt.deptheader "View reports for ${academicYear.toString}" "in" department routes.reports "deptheaderroute" />

<#if (academicYear.startYear >= 2014)>
	<h2>Monitoring points</h2>

	<ul>
		<li><h3><a href="<@routes.reports.allAttendance department academicYear />">All attendance</a></h3></li>
		<li><h3><a href="<@routes.reports.unrecordedAttendance department academicYear />">Unrecorded monitoring points</a></h3></li>
		<li><h3><a href="<@routes.reports.missedAttendance department academicYear />">Missed monitoring points</a></h3></li>
	</ul>
</#if>

<h2>Profiles</h2>

<ul>
	<li><h3><a href="<@routes.reports.profileExport department academicYear />">Export profiles</a></h3></li>
</ul>

<h2>Small group teaching</h2>

<ul>
	<li><h3><a href="<@routes.reports.allSmallGroups department academicYear />">All event attendance</a></h3></li>
	<li><h3><a href="<@routes.reports.unrecordedSmallGroups department academicYear />">Unrecorded event attendance</a></h3></li>
	<li><h3><a href="<@routes.reports.unrecordedSmallGroupsByModule department academicYear />">Unrecorded event attendance by module</a></h3></li>
	<li><h3><a href="<@routes.reports.missedSmallGroups department academicYear />">Missed event attendance</a></h3></li>
	<li><h3><a href="<@routes.reports.missedSmallGroupsByModule department academicYear />">Missed event attendance by module</a></h3></li>
</ul>


</#escape>