<#escape x as x?html>

<div class="modal-header">
	<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
	<h2>Create monitoring point</h2>
</div>

<div class="modal-body">

	<#assign action><@url page="/sysadmin/pointsettemplates/${command.template.id}/edit/points/add" /></#assign>

	<@f.form id="createMonitoringPoint" action="${action}" method="POST" commandName="command" class="form-horizontal">
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