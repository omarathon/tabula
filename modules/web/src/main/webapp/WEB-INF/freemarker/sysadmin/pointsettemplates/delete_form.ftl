<#escape x as x?html>

<h1>Delete monitoring point set template</h1>

<form id="deleteMonitoringPointSet" action="<@url page="/sysadmin/pointsettemplates/${command.template.id}/delete"/>" method="POST" class="form-inline">

	<p>You are deleting the monitoring point set template: ${command.template.templateName}.</p>

	<p>
		<@form.label checkbox=true>
			<@f.checkbox path="command.confirm" /> I confirm that I want to delete this monitoring point set template and all its points.
		</@form.label>
		<@form.errors path="command.confirm"/>
	</p>

	<input type="submit" value="Delete" class="btn btn-primary"/> <a class="btn" href="<@url page="/sysadmin/pointsettemplates"/>">Cancel</a>

</form>

</#escape>