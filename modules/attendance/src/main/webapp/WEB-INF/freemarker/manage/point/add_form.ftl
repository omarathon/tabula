<#escape x as x?html>
<#import "*/modal_macros.ftl" as modal />

<@modal.header>
	<h2>New monitoring point</h2>
</@modal.header>

<@modal.body>

	<#assign action><@url page="/manage/${command.dept.code}/sets/add/points/add" /></#assign>

	<@f.form id="newMonitoringPoint" action="${action}" method="POST" commandName="command" class="form-horizontal">
		<#list command.monitoringPoints as point>
			<input type="hidden" name="monitoringPoints[${point_index}].name" value="${point.name}" />
			<input type="hidden" name="monitoringPoints[${point_index}].validFromWeek" value="${point.validFromWeek}" />
			<input type="hidden" name="monitoringPoints[${point_index}].requiredFromWeek" value="${point.requiredFromWeek}" />
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