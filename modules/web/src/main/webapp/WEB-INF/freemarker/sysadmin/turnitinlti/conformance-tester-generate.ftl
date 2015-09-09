<#escape x as x?html>

<h1>LTI Conformance tester (basic-lti-launch-request)</h1>

	<@f.form method="post" action="${url('/sysadmin/turnitinlti/conformancetester-generate')}" commandName="ltiConformanceTesterPopulateFormCommand" cssClass="form-horizontal">

		<@f.errors cssClass="error form-errors" />

		<@form.labelled_row "endpoint" "endpoint for testing">
			<@f.input path="endpoint" cssClass="text" />
		</@form.labelled_row>

		<@form.labelled_row "key" "consumer key for testing">
			<@f.input path="key" cssClass="text" />
		</@form.labelled_row>

		<@form.labelled_row "secret" "consumer secret for testing">
			<@f.input path="secret" cssClass="text" />
		</@form.labelled_row>

		<@form.labelled_row "givenName" "given name">
			<@f.input path="givenName" cssClass="text" />
		</@form.labelled_row>

		<@form.labelled_row "familyName" "family name">
			<@f.input path="familyName" cssClass="text" />
		</@form.labelled_row>

		<@form.labelled_row "role" "role">
			<@f.input path="role" value="Instructor" cssClass="text" />
		</@form.labelled_row>

		<@form.labelled_row "mentee" "Mentee">
			<@f.input path="mentee" cssClass="text" />
		</@form.labelled_row>

		<@form.labelled_row "email" "Email">
			<@f.input path="email" value="sian@imscert.org" cssClass="text" />
		</@form.labelled_row>

		<@form.labelled_row "tool_consumer_info_version" "Tabula version">
			<@f.input path="tool_consumer_info_version" value="113" cssClass="text" />
		</@form.labelled_row>

		<@form.labelled_row "assignment" "Tabula assignment id">
			<@f.input path="assignment" cssClass="text" />
		</@form.labelled_row>

		<@form.row>
			<@form.label path="customParams">Custom params</@form.label>
			<@form.field>
				<@f.textarea path="customParams" cssClass="big-textarea" />
			</@form.field>
		</@form.row>

	<div class="submit-buttons">
		<input type="submit" value="Send" class="btn btn-primary">
		<a class="btn" href="<@url page="/sysadmin/turnitinlti" />">Cancel</a>
	</div>

	</@f.form>

</#escape>