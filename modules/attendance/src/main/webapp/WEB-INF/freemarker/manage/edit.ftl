<#escape x as x?html>
<#import "../attendance_macros.ftl" as attendance_macros />

<#macro listStudentIdInputs>
	<#list command.staticStudentIds as id>
	<input type="hidden" name="staticStudentIds" value="${id}" />
	</#list>
	<#list command.includedStudentIds as id>
	<input type="hidden" name="includedStudentIds" value="${id}" />
	</#list>
	<#list command.excludedStudentIds as id>
	<input type="hidden" name="excludedStudentIds" value="${id}" />
	</#list>
</#macro>

<h1>Edit scheme</h1>

<@f.form id="editScheme" method="POST" commandName="command" class="form-horizontal">

	<@form.labelled_row "name" "Name">
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
			<#if command.scheme.pointStyle.dbValue == "week">
				term weeks
			<#else>
				calendar dates
			</#if>
			<span class="hint">You cannot change the type of points once some points have been added to a scheme</span>
		</@form.labelled_row>
	</#if>


<#assign membershipItems = command.membershipItems />

<#if membershipItems?size == 0>
	<p>Select which students this scheme should apply to</p>

	<p>
		No students on this scheme

		<#assign popoverContent><#noescape>
			You can filter to select types of students (e.g. 1st year part-time UG)
			and then either use these to populate a static list (which will not then change),
			or link this group to SITS so that the list of students will be updated automatically from there.
			You can also manually add students by ITS usercode or university number.

			You can tweak the list even when it is linked to SITS, by manually adding and excluding students.
		</#noescape></#assign>
		<@fmt.help_popover id="student-count" content="${popoverContent}" />
	</p>

	<button type="button" class="btn" onclick="document.forms.selectStudents.submit()">Select students for scheme</button>
<#else>
	<details>
		<summary class="large-chevron collapsible">
			<span class="legend">Students
				<small>Select which students this scheme should apply to</small>
			</span>

			<p><@fmt.p membershipItems?size "student" /> on this scheme</p>

			<button type="button" class="btn" onclick="document.forms.selectStudents.submit()">Select students for scheme</button>

			<@spring.bind path="staticStudentIds">
				<#if status.error>
					<div class="alert alert-error"><@f.errors path="staticStudentIds" cssClass="error"/></div>
				</#if>
			</@spring.bind>
		</summary>

		<@attendance_macros.manageStudentTable membershipItems />
	</details>

</#if>

<p>&nbsp;</p>
	<@listStudentIdInputs />
	<input type="hidden" name="filterQueryString" value="${command.filterQueryString!""}" />
	<input
		type="submit"
		class="btn btn-primary"
		name="create"
		value="Save"
	/>
	<a class="btn" href="<@routes.manageHomeForYear command.scheme.department command.scheme.academicYear.startYear?c />">Cancel</a>

</@f.form>

<form id="selectStudents" method="POST" action="<@routes.manageSelectStudents command.scheme />">
	<@listStudentIdInputs />
	<input type="hidden" name="filterQueryString" value="${command.filterQueryString!""}"/>
	<input type="hidden" name="manageSchemeUrl" value="<@routes.manageEditScheme command.scheme.department command.scheme.academicYear.startYear?c command.scheme/>">
</form>
</#escape>