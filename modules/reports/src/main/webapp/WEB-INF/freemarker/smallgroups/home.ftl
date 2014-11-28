<#escape x as x?html>

<h1>Small group teaching</h1>

<ul>
	<li><h3><a href="<@routes.allSmallGroups department academicYear />">All event attendance</a></h3></li>
	<li><h3><a href="<@routes.unrecordedSmallGroups department academicYear />">Unrecorded event attendance</a></h3></li>
	<li><h3><a href="<@routes.unrecordedSmallGroupsByModule department academicYear />">Unrecorded event attendance by module</a></h3></li>
	<li><h3><a href="<@routes.missedSmallGroups department academicYear />">Missed event attendance</a></h3></li>
	<li><h3><a href="<@routes.missedSmallGroupsByModule department academicYear />">Missed event attendance by module</a></h3></li>
</ul>
</#escape>