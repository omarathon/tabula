<#escape x as x?html>
<#import "*/group_components.ftl" as components />
	<h1>Edit small groups</h1>
	<h4><span class="muted">for</span> <@fmt.module_name module /></h4>

	<@f.form id="editEvents" method="POST" commandName="command" class="form-horizontal">
		<@components.set_wizard false 'allocate' smallGroupSet />

		<#assign submitUrl><@routes.editsetallocate smallGroupSet /></#assign>
		<#include "_allocate.ftl" />
	</@f.form>
</#escape>