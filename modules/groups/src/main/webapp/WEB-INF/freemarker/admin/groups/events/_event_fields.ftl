<#escape x as x?html>
	<#import "*/group_components.ftl" as components />

	<fieldset>
		<@form.labelled_row "title" "Title">
			<@f.input path="title" />
		</@form.labelled_row>

		<@form.labelled_row "tutors" "Tutors">
			<@form.flexipicker path="tutors" placeholder="User name" list=true multiple=true auto_multiple=false />
		</@form.labelled_row>

		<@components.week_selector "weeks" allTerms smallGroupSet />

		<@form.labelled_row "day" "Day">
			<@f.select path="day" id="day">
				<@f.option value="" label=""/>
				<@f.options items=allDays itemLabel="name" itemValue="asInt" />
			</@f.select>
		</@form.labelled_row>

		<#-- The time-picker causes the entire page to become a submit button, can't work out why -->
		<@form.labelled_row "startTime" "Start time">
			<@f.input path="startTime" cssClass="time-picker startDateTime" />
			<input class="endoffset" type="hidden" data-end-offset="3600000" />
		</@form.labelled_row>

		<@form.labelled_row "endTime" "End time">
			<@f.input path="endTime" cssClass="time-picker endDateTime" />
		</@form.labelled_row>

		<@form.labelled_row "location" "Location">
			<@f.hidden path="locationId" />
			<@f.input path="location" />
		</@form.labelled_row>
	</fieldset>

	<style type="text/css">
		<#-- Hide the confusing dates in the header of the time picker -->
		.datetimepicker-hours thead i { display: none !important; }
		.datetimepicker-hours thead .switch { visibility: hidden; }
		.datetimepicker-hours thead th { height: 0px; }
		.datetimepicker-minutes thead .switch { visibility: hidden; }
	</style>
</#escape>