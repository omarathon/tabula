<#escape x as x?html>

<h1>Turnitin LTI - view report</h1>

	<@f.form method="post" action="${url('/sysadmin/turnitinlti/viewreport')}" commandName="turnitinLtiViewReportCommand" cssClass="form-horizontal">

		<@f.errors cssClass="error form-errors" />

		<@form.labelled_row "assignment" "Assignment">
			<@f.input path="assignment" cssClass="text" />
		</@form.labelled_row>

		<@form.labelled_row "attachment" "Attachment">
			<@f.input path="attachment" cssClass="text" value=""/>
		</@form.labelled_row>

	<div class="submit-buttons">
		<input type="submit" value="Save" class="btn btn-primary">
		<a class="btn" href="<@url page="/sysadmin/turnitinlti" />">Cancel</a>
	</div>

	</@f.form>

</#escape>