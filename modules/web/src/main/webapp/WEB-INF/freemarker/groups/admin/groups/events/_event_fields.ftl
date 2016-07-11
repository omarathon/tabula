<#escape x as x?html>
	<#import "*/group_components.ftl" as components />

	<@bs3form.labelled_form_group path="title" labelText="Title">
		<@f.input path="title" cssClass="form-control" />
	</@bs3form.labelled_form_group>

	<@bs3form.labelled_form_group path="tutors" labelText="Tutors">
		<@form.flexipicker path="tutors" placeholder="User name" list=true multiple=true auto_multiple=false />
	</@bs3form.labelled_form_group>

	<@components.week_selector "weeks" allTerms smallGroupSet />

	<@bs3form.labelled_form_group path="day" labelText="Day">
		<@f.select path="day" id="day" cssClass="form-control">
			<@f.option value="" label=""/>
			<@f.options items=allDays itemLabel="name" itemValue="asInt" />
		</@f.select>
	</@bs3form.labelled_form_group>

	<#-- The time-picker causes the entire page to become a submit button, can't work out why -->
	<div class="dateTimePair">
		<@bs3form.labelled_form_group path="startTime" labelText="Start time">
			<@f.input path="startTime" cssClass="time-picker startDateTime form-control" />
			<input class="endoffset" type="hidden" data-end-offset="3600000" />
		</@bs3form.labelled_form_group>

		<@bs3form.labelled_form_group path="endTime" labelText="End time">
			<@f.input path="endTime" cssClass="time-picker endDateTime form-control" />
		</@bs3form.labelled_form_group>
	</div>

	<@bs3form.labelled_form_group path="location" labelText="Location">
		<@f.hidden path="locationId" />
		<@f.input path="location" cssClass="form-control" />
	</@bs3form.labelled_form_group>

	<@bs3form.labelled_form_group path="link" labelText="Link">
		<@f.input path="link" cssClass="form-control" />
	</@bs3form.labelled_form_group>

	<@bs3form.labelled_form_group path="linkText" labelText="Link Text">
		<@f.input path="linkText" cssClass="form-control" />
	</@bs3form.labelled_form_group>

	<style type="text/css">
		<#-- Hide the confusing dates in the header of the time picker -->
		.datetimepicker-hours thead i { display: none !important; }
		.datetimepicker-hours thead .switch { visibility: hidden; }
		.datetimepicker-hours thead th { height: 0px; }
		.datetimepicker-minutes thead .switch { visibility: hidden; }
	</style>
</#escape>