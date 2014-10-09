<#escape x as x?html>
<#import "*/group_components.ftl" as components />
	<h1>Create small groups</h1>
	<h4><span class="muted">for</span> <@fmt.module_name module /></h4>

	<div id="editEvents">
		<@components.set_wizard true 'allocate' smallGroupSet />

		<#assign submitUrl><@routes.createsetallocate smallGroupSet /></#assign>
		<#include "_allocate.ftl" />
	</div>
</#escape>