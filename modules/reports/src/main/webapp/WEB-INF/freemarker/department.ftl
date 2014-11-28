<#escape x as x?html>

<@fmt.deptheader "View reports" "in" department routes "department" />

<ul>
	<#list academicYears as year>
		<li><h3><a href="<@routes.departmentWithYear department year />">${department.name} ${year.toString}</a></h3></li>
	</#list>
</ul>

</#escape>