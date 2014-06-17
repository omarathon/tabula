<#escape x as x?html>

<h1>Create a scheme</h1>

<@f.form id="newScheme" method="POST" commandName="command" class="form-horizontal">

	<p class="progress-arrows">
		<span class="arrow-right active">Properties</span>
		<span class="arrow-right arrow-left use-tooltip" title="Save and edit students"><button type="submit" class="btn btn-link" name="${ManageSchemeMappingParameters.createAndAddStudents}">Students</button></span>
		<span class="arrow-right arrow-left use-tooltip" title="Save and edit points"><button type="submit" class="btn btn-link" name="${ManageSchemeMappingParameters.createAndAddPoints}">Points</button></span>
	</p>

	<@form.labelled_row "name" "Name">
		<@f.input path="name" />
		<@fmt.help_popover id="name" content="Give the scheme an optional name to distinguish it from other schemes in your department e.g. 1st Year Undergrads (part-time)" />
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
		class="btn btn-success use-tooltip"
		name="${ManageSchemeMappingParameters.createAndAddStudents}"
		value="Add students"
		title="Select which students this scheme should apply to"
		data-container="body"
	/>
	<input
		type="submit"
		class="btn btn-primary use-tooltip"
		name="create"
		value="Save"
		title="Save your blank scheme and add students and points to it later"
		data-container="body"
	/>
	<a class="btn" href="<@routes.manageHomeForYear command.department command.academicYear.startYear?c />">Cancel</a>
</@f.form>
</#escape>