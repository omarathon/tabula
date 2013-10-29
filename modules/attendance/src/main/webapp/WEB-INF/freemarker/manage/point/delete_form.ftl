<#escape x as x?html>
<#import "*/modal_macros.ftl" as modal />

<@modal.header>
	<h2>Delete monitoring point</h2>
</@modal.header>

<@modal.body>

	<#assign action><@routes.deletePoint command.dept command.pointIndex /></#assign>

	<@f.form id="deleteMonitoringPoint" action="${action}" method="POST" commandName="command" class="form-horizontal">
		<#list command.monitoringPoints as point>
			<#include "_hidden_fields.ftl" />
		</#list>
		<#assign point = command.monitoringPoints[pointIndex] />
		<p>You are deleting the monitoring point: ${point.name} (<@fmt.monitoringPointWeeksFormat point.validFromWeek point.requiredFromWeek command.academicYear command.dept />).</p>

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