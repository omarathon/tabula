<#escape x as x?html>

<div class="modal-header">
	<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
	<h2>Update monitoring point</h2>
</div>

<div class="modal-body">

	<#assign action><@url page="/manage/${command.set.route.department.code}/sets/${command.set.id}/points/${command.point.id}/edit" /></#assign>

	<@f.form id="updateMonitoringPoint" action="${action}" method="POST" commandName="command" class="form-horizontal">
		<@spring.bind path="command">
			<#if status.error>
				<div class="alert alert-error"><@f.errors /></div>
			</#if>
		</@spring.bind>
		<#include "_fields.ftl" />
	</@f.form>

</div>

<div class="modal-footer">
	<button class="btn btn-primary spinnable spinner-auto" type="submit" name="submit" data-loading-text="Updating&hellip;">
		Update
	</button>
	<button class="btn" data-dismiss="modal" aria-hidden="true">Cancel</button>
</div>

</#escape>