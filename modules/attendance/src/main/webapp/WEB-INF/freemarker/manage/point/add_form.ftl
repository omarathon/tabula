<#escape x as x?html>

<div class="modal-header">
	<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
	<h2>New monitoring point</h2>
</div>

<div class="modal-body">

	<#assign action><@url page="/manage/${command.dept.code}/sets/add/points/add" /></#assign>

	<@f.form id="newMonitoringPoint" action="${action}" method="POST" commandName="command" class="form-horizontal">
		<#list command.monitoringPoints as point>
			<input type="hidden" name="monitoringPoints[${point_index}].name" value="${point.name}" />
			<input type="hidden" name="monitoringPoints[${point_index}].defaultValue" value="<#if point.defaultValue>true<#else>false></#if>" />
			<input type="hidden" name="monitoringPoints[${point_index}].week" value="${point.week}" />
		</#list>
		<#include "_fields.ftl" />
	</@f.form>

</div>

<div class="modal-footer">
	<button class="btn btn-primary spinnable spinner-auto" type="submit" name="submit" data-loading-text="Creating&hellip;">
		Create
	</button>
	<button class="btn" data-dismiss="modal" aria-hidden="true">Cancel</button>
</div>

</#escape>