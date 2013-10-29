<#escape x as x?html>
<#import "*/modal_macros.ftl" as modal />

<@modal.header>
	<h2>New monitoring point</h2>
</@modal.header>

<@modal.body>

	<#assign action><@routes.addPoint command.dept /></#assign>

	<@f.form id="newMonitoringPoint" action="${action}" method="POST" commandName="command" class="form-horizontal">
		<#list command.monitoringPoints as point>
			<#include "_hidden_fields.ftl" />
		</#list>
		<#include "_fields.ftl" />
	</@f.form>

</@modal.body>

<@modal.footer>
	<button class="btn btn-primary spinnable spinner-auto" type="submit" name="submit" data-loading-text="Creating&hellip;">
		Create
	</button>
	<button class="btn" data-dismiss="modal" aria-hidden="true">Cancel</button>
</@modal.footer>

</#escape>