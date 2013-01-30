<h1>Maintenance mode</h1>

<@f.form action="?" cssClass="form-horizontal" commandName="maintenanceModeCommand">

	<@form.labelled_row "enable" "Enabled?">
		<@f.checkbox path="enable" />
	</@form.labelled_row>
	
	<@form.labelled_row "until" "ETA">
		<@f.input path="until" cssClass="date-time-picker" placeholder="Click to pick a date" />
	</@form.labelled_row>
	
	<@form.labelled_row "message" "Custom message">
		<@f.textarea path="message" />
	</@form.labelled_row>
	
	<input class="btn btn-danger" type="submit" value="Update" />
	
</@f.form>