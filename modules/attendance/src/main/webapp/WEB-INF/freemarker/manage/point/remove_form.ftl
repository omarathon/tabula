<#escape x as x?html>

<div class="modal-header">
	<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
	<h2>Delete monitoring point</h2>
</div>

<div class="modal-body">

	<#assign action><@url page="/manage/${command.set.route.department.code}/sets/${command.set.id}/points/${command.point.id}/delete" /></#assign>

	<@f.form id="deleteMonitoringPoint" action="${action}" method="POST" commandName="command" class="form-horizontal">
		<@spring.bind path="command">
			<#if status.error>
				<div class="alert alert-error"><@f.errors /></div>
			</#if>
		</@spring.bind>

		<p>You are deleting the monitoring point: ${command.point.name} (<@fmt.weekRanges point />).</p>

		<p>
			<@form.label checkbox=true>
				<@f.checkbox path="confirm" /> I confirm that I want to delete this monitoring point.
			</@form.label>
			<@form.errors path="confirm"/>
		</p>

	</@f.form>

</div>
<div class="modal-footer">
	<button class="btn btn-primary spinnable spinner-auto" type="submit" name="submit" data-loading-text="Deleting&hellip;">
		Delete
	</button>
	<button class="btn" data-dismiss="modal" aria-hidden="true">Cancel</button>
</div>


</#escape>