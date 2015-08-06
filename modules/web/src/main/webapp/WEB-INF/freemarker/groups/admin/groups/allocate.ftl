<#escape x as x?html>
	<h1>Allocate students</h1>
	<h4><span class="muted">to</span> ${smallGroupSet.name}</h4>

	<#assign submitUrl><@routes.groups.allocateset smallGroupSet /></#assign>
	<#include "_allocate.ftl" />
</#escape>