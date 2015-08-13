<#escape x as x?html>

<h1>Turnitin LTI - submit a test paper</h1>

	<@f.form method="post" action="${url('/sysadmin/turnitinlti/submitpaper')}" commandName="turnitinLtiSubmitPaperCommand" cssClass="form-horizontal">

		<@f.errors cssClass="error form-errors" />

		<@form.labelled_row "assignment" "Assignment">
			<@f.input path="assignment" cssClass="text" value=""/>
		</@form.labelled_row>

		<@form.labelled_row "attachment" "Attachment">
			<@f.input path="attachment" cssClass="text" value=""/>
		</@form.labelled_row>

		<@form.labelled_row "paperUrl" "URL to paper">
			<@f.input path="paperUrl" cssClass="text" value=""/>
		</@form.labelled_row>


	<div class="submit-buttons">
		<input type="submit" value="Save" class="btn btn-primary">
		<a class="btn" href="<@url page="/sysadmin/turnitinlti" />">Cancel</a>
	</div>

	</@f.form>

</#escape>