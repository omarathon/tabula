<#escape x as x?html>

<div class="modal-header">
	<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
	<h2>Edit monitoring point</h2>
</div>

<div class="modal-body">

	<#assign action><@url page="/sysadmin/pointsettemplates/add/points/edit/${command.pointIndex}" /></#assign>

	<@f.form id="editMonitoringPoint" action="${action}" method="POST" commandName="command" class="form-horizontal">
		<#list command.monitoringPoints as point>
			<input type="hidden" name="monitoringPoints[${point_index}].name" value="${point.name}" />
			<input type="hidden" name="monitoringPoints[${point_index}].validFromWeek" value="${point.validFromWeek}" />
			<input type="hidden" name="monitoringPoints[${point_index}].requiredFromWeek" value="${point.requiredFromWeek}" />
		</#list>
		<#include "_fields.ftl" />
	</@f.form>

</div>

<div class="modal-footer">
	<button class="btn btn-primary spinnable spinner-auto" type="submit" name="submit" data-loading-text="Editing&hellip;">
		Edit
	</button>
	<button class="btn" data-dismiss="modal" aria-hidden="true">Cancel</button>
</div>

</#escape>