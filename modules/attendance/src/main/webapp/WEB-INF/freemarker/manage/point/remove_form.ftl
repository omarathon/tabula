<#escape x as x?html>
<#import "*/modal_macros.ftl" as modal />

<@modal.header>
	<h2>Delete monitoring point</h2>
</@modal.header>

<@modal.body>

	<#assign action><@url page="/manage/${command.set.route.department.code}/sets/${command.set.id}/edit/points/${command.point.id}/delete" /></#assign>

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

</@modal.body>

<@modal.footer>
	<button class="btn btn-primary spinnable spinner-auto" type="submit" name="submit" data-loading-text="Deleting&hellip;">
		Delete
	</button>
	<button class="btn" data-dismiss="modal" aria-hidden="true">Cancel</button>
</@modal.footer>


</#escape>