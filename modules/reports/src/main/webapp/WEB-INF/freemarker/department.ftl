<#escape x as x?html>

<h2>View reports</h2>
<ul>
	<#list academicYears as year>
		<li><h3><a href="<@routes.departmentWithYear department year />">${department.name} ${year.toString}</a></h3></li>
	</#list>
</ul>

</#escape>