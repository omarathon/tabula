<#escape x as x?html>
<#import "*/group_components.ftl" as components />
	<div class="deptheader">
		<h1>Create small groups</h1>
		<h4 class="with-related"><span class="muted">for</span> <@fmt.module_name module /></h4>
	</div>

	<div id="editEvents">
		<@components.set_wizard true 'allocate' smallGroupSet />

		<#assign submitUrl><@routes.groups.createsetallocate smallGroupSet /></#assign>
		<#include "_allocate.ftl" />
	</div>
</#escape>