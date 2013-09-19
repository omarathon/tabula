<#escape x as x?html>
<#import "*/modal_macros.ftl" as modal />

<@modal.header>
	<h2>Edit monitoring point</h2>
</@modal.header>

<@modal.body>

	<#assign action><@url page="/manage/${command.dept.code}/sets/add/points/edit/${command.pointIndex}" /></#assign>

	<@f.form id="editMonitoringPoint" action="${action}" method="POST" commandName="command" class="form-horizontal">
		<#list command.monitoringPoints as point>
			<input type="hidden" name="monitoringPoints[${point_index}].name" value="${point.name}" />
			<input type="hidden" name="monitoringPoints[${point_index}].defaultValue" value="<#if point.defaultValue>true<#else>false</#if>" />
			<input type="hidden" name="monitoringPoints[${point_index}].week" value="${point.week}" />
		</#list>
		<#include "_fields.ftl" />
	</@f.form>

</@modal.body>

<@modal.footer>
	<button class="btn btn-primary spinnable spinner-auto" type="submit" name="submit" data-loading-text="Editing&hellip;">
		Edit
	</button>
	<button class="btn" data-dismiss="modal" aria-hidden="true">Cancel</button>
</@modal.footer>

</#escape>