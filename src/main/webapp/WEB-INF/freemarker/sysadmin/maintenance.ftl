<h1>Maintenance mode</h1>

<#assign formurl><@url page="/sysadmin/maintenance"/></#assign>
<@f.form action=formurl cssClass="form-horizontal" commandName="maintenanceModeForm">

	<@form.labelled_row "enable" "Enabled?">
		<@f.checkbox path="enable" />
	</@form.labelled_row>
	
	<@form.labelled_row "until" "ETA">
		<@f.input path=path cssClass="date-time-picker" placeholder="Click to pick a date" />
	</@form.labelled_row>
	
	<@form.labelled_row "message" "Custom message">
		<@f.textarea path="message" />
	</@form.labelled_row>
	
	<input class="btn btn-danger" type="submit" value="Update" />
	
</@f.form>