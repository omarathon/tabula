<#escape x as x?html>
<#import "*/group_components.ftl" as components />
	<h1>Edit small groups</h1>
	<h4><span class="muted">for</span> <@fmt.module_name module /></h4>

	<p class="progress-arrows">
		<span class="arrow-right">Properties</span>
		<span class="arrow-right arrow-left">Students</span>
		<span class="arrow-right arrow-left">Groups</span>
		<span class="arrow-right arrow-left">Events</span>
		<span class="arrow-right arrow-left active">Allocate</span>
	</p>

	<#assign submitUrl><@routes.editsetallocate smallGroupSet /></#assign>
	<#include "_allocate.ftl" />
</#escape>