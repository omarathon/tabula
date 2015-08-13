<#escape x as x?html>

<h1>LTI Conformance tester (basic-lti-launch-request)</h1>

	<@f.form method="post" action="${url('/sysadmin/turnitinlti/conformancetester')}" commandName="ltiConformanceTesterCommand" cssClass="form-horizontal">

		<@f.errors cssClass="error form-errors" />

		<@form.labelled_row "endpoint" "endpoint for testing">
			<@f.input path="endpoint" cssClass="text" />
		</@form.labelled_row>

		<@form.labelled_row "secret" "secret for testing">
			<@f.input path="secret" cssClass="text" />
		</@form.labelled_row>

	<div class="submit-buttons">
		<input type="submit" value="Save" class="btn btn-primary">
		<a class="btn" href="<@url page="/sysadmin/turnitinlti" />">Cancel</a>
	</div>

	</@f.form>

</#escape>