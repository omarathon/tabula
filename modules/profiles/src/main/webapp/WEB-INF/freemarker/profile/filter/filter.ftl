<#escape x as x?html>
	<h1>View students</h1>
	<h4><span class="muted">in</span> ${department.name}</h4>

	<#assign filterCommand = filterStudentsCommand />
	<#assign filterCommandName = "filterStudentsCommand" />
	<#assign filterResultsPath = "/WEB-INF/freemarker/profile/filter/results.ftl" />
	<#assign submitUrl><@routes.filter_students department /></#assign>
	<#include "/WEB-INF/freemarker/filter_bar.ftl" />
</#escape>