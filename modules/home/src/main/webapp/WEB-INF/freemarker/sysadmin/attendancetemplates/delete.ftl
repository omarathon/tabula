<#escape x as x?html>

<h1>Delete template</h1>

<form id="deleteMonitoringPointSet" action="<@url page="/sysadmin/attendancetemplates/${template.id}/delete"/>" method="POST" class="form-inline">

	<p>You are deleting template: ${template.templateName}.</p>

	<p>
		<@form.label checkbox=true>
			<@f.checkbox path="command.confirm" /> I confirm that I want to delete this template and all its points.
		</@form.label>
		<@form.errors path="command.confirm"/>
	</p>

	<input type="submit" value="Delete" class="btn btn-danger"/> <a class="btn" href="<@url page="/sysadmin/attendancetemplates"/>">Cancel</a>

</form>

</#escape>