<#escape x as x?html>
<#import "../attendance_macros.ftl" as attendance_macros />

<h1>Edit scheme: ${scheme.displayName}</h1>

<@f.form id="editScheme" method="POST" commandName="command" class="form-horizontal">

	<p class="progress-arrows">
		<span class="arrow-right active">Properties</span>
		<span class="arrow-right arrow-left use-tooltip" title="Save and edit students"><button type="submit" class="btn btn-link" name="${ManageSchemeMappingParameters.createAndAddStudents}">Students</button></span>
		<span class="arrow-right arrow-left use-tooltip" title="Save and edit points"><button type="submit" class="btn btn-link" name="${ManageSchemeMappingParameters.createAndAddPoints}">Points</button></span>
	</p>

	<@form.labelled_row "name" "Scheme name">
		<@f.input path="name" />
		<@fmt.help_popover id="name" content="Give the scheme an optional name to distinguish it from other schemes in your department e.g. 1st Year Undergrads (part-time)" />
	</@form.labelled_row>

	<#if command.scheme.points?size == 0>
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
	<#else>
		<@form.labelled_row "pointStyle" "Date format">
			<@f.hidden path="pointStyle" />
			<@form.label clazz="radio" checkbox=true>
				<@f.radiobutton path="pointStyle" value="week" disabled="true" />
					term weeks
				<@fmt.help_popover id="pointStyle-week" content="Create points which cover term weeks e.g. Personal tutor meeting weeks 2-3" />
			</@form.label>
			<@form.label clazz="radio" checkbox=true>
				<@f.radiobutton path="pointStyle" value="date" disabled="true" />
				calendar dates
				<@fmt.help_popover id="pointStyle-date" content="Create points which use calendar dates e.g. Supervision 1st-31st October" />
			</@form.label>
			<span class="hint">You cannot change the type of points once some points have been added to a scheme</span>
		</@form.labelled_row>
	</#if>

	<input
		type="submit"
		class="btn btn-success use-tooltip"
		name="${ManageSchemeMappingParameters.createAndAddStudents}"
		value="Save"
		title="Select which students this scheme should apply to"
		data-container="body"
	/>
	<input
		type="submit"
		class="btn btn-primary use-tooltip"
		name="create"
		value="Save and exit"
		title="Save your scheme"
		data-container="body"
	/>

	<a class="btn" href="<@routes.manageHomeForYear command.scheme.department command.scheme.academicYear.startYear?c />">Cancel</a>

</@f.form>

</#escape>