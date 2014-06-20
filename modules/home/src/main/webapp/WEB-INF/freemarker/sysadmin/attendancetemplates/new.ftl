<#escape x as x?html>

<h1>Create a template</h1>

<#assign action><@url page="/sysadmin/attendancetemplates/add"/></#assign>
<@f.form id="newScheme" method="POST" commandName="command" class="form-horizontal" action="${action}">

	<@form.labelled_row "name" "Name">
		<@f.input path="name" />
	</@form.labelled_row>

	<@form.labelled_row "pointStyle" "Date format">
		<@form.label clazz="radio" checkbox=true>
			<@f.radiobutton path="pointStyle" value="week" />
			term weeks
			<@fmt.help_popover id="pointStyle-week" content="Create points which cover term weeks e.g. Personal tutor meeting weeks 2-3" />
		</@form.label>
		<@form.label clazz="radio" checkbox=true>
			<@f.radiobutton path="pointStyle" value="date" />
			calendar dates
			<@fmt.help_popover id="pointStyle-date" content="Create points which use calendar dates e.g. Supervision 1st-31st October" />
		</@form.label>
		<span class="hint">Select the date format to use for points on this scheme</span>
	</@form.labelled_row>

	<input
		type="submit"
		class="btn btn-primary"
		name="create"
		value="Save"
		data-container="body"
	/>
	<a class="btn" href="<@url page="/sysadmin/attendancetemplates" />">Cancel</a>
</@f.form>
</#escape>