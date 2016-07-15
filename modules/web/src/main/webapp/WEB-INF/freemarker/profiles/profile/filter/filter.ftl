<#escape x as x?html>
	<div class="deptheader">
		<h1>View students</h1>
		<h4 class="with-related"><span class="muted">in</span> ${department.name}</h4>
	</div>

	<#assign filterCommand = filterStudentsCommand />
	<#assign filterCommandName = "filterStudentsCommand" />
	<#assign filterResultsPath = "/WEB-INF/freemarker/profiles/profile/filter/results.ftl" />
	<#assign submitUrl><@routes.profiles.filter_students department /></#assign>
	<#include "/WEB-INF/freemarker/filter_bar.ftl" />
</#escape>