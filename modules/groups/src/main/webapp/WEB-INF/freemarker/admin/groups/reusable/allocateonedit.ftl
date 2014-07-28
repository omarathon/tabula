<#escape x as x?html>
	<h1>Edit reusable small groups: ${smallGroupSet.name}</h1>

	<#assign submitUrl><@routes.editcrossmodulegroupsallocate smallGroupSet /></#assign>
	<#include "_allocate.ftl" />
</#escape>