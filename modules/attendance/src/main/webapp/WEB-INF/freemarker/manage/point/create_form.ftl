<#escape x as x?html>
<#import "*/modal_macros.ftl" as modal />

<@modal.header>
	<h2>Create monitoring point</h2>
</@modal.header>

<@modal.body>

	<#assign action><@routes.createPoint command.set /></#assign>

	<@f.form id="createMonitoringPoint" action="${action}" method="POST" commandName="command" class="form-horizontal">
		<@spring.bind path="command">
			<#if status.error>
				<div class="alert alert-error"><@f.errors path="command" cssClass="error"/></div>
			</#if>
		</@spring.bind>
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