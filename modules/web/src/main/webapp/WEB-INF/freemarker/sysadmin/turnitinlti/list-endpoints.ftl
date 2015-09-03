<#escape x as x?html>

<h1>Turnitin LTI - list endpoints for an assignment</h1>

	<@f.form method="post" action="${url('/sysadmin/turnitinlti/listendpoints')}" commandName="turnitinLtiListEndpointsCommand" cssClass="form-horizontal">

		<@f.errors cssClass="error form-errors" />

		<@form.labelled_row "turnitinAssignmentId" "Turnitin Assignment ID">
			<@f.input path="turnitinAssignmentId" cssClass="text" />
		</@form.labelled_row>

	<div class="submit-buttons">
		<input type="submit" value="Save" class="btn btn-primary">
		<a class="btn" href="<@url page="/sysadmin/turnitinlti" />">Cancel</a>
	</div>

	</@f.form>

</#escape>