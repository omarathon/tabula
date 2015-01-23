<#escape x as x?html>

<#if (academicYear.startYear >= 2014)>
	<h1>Monitoring points</h1>

	<ul>
		<li><h3><a href="<@routes.allAttendance department academicYear />">All attendance</a></h3></li>
		<li><h3><a href="<@routes.unrecordedAttendance department academicYear />">Unrecorded monitoring points</a></h3></li>
		<li><h3><a href="<@routes.missedAttendance department academicYear />">Missed monitoring points</a></h3></li>
	</ul>
<#else>
	<div class="alert alert-warning">
		Monitoring point reports are not available for 13/14
	</div>
</#if>
</#escape>