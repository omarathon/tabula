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

<h1>Edit ${scheme.displayName}</h1>

<div class="fix-area">

	<@f.form id="editScheme" method="POST" commandName="command" class="form-horizontal">

		<p class="progress-arrows">
			<span class="arrow-right use-tooltip" title="Save and edit properties"><button type="submit" class="btn btn-link" name="${ManageSchemeMappingParameters.saveAndEditProperties}">Properties</button></span>
			<span class="arrow-right arrow-left use-tooltip active">Students</span>
			<span class="arrow-right arrow-left use-tooltip" title="Save and edit points"><button type="submit" class="btn btn-link" name="${ManageSchemeMappingParameters.createAndAddPoints}">Points</button></span>
		</p>

		<#assign membershipItems = command.membershipItems />

		<#if membershipItems?size == 0>

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

			<p><@fmt.p membershipItems?size "student" /> on this scheme</p>

			<p><button type="button" class="btn" onclick="document.forms.selectStudents.submit()">Select students for scheme</button></p>

			<@spring.bind path="staticStudentIds">
				<#if status.error>
					<div class="alert alert-error"><@f.errors path="staticStudentIds" cssClass="error"/></div>
				</#if>
			</@spring.bind>

			<@attendance_macros.manageStudentTable membershipItems />

		</#if>

		<div class="fix-footer submit-buttons">

			<@listStudentIdInputs />
			<input type="hidden" name="filterQueryString" value="${command.filterQueryString!""}" />
			<input
				type="submit"
				class="btn btn-primary"
				name="create"
				value="Save"
			/>
			<a class="btn" href="<@routes.manageHomeForYear command.scheme.department command.scheme.academicYear.startYear?c />">Cancel</a>

		</div>

	</@f.form>
</div>

<form id="selectStudents" method="POST" action="<@routes.manageSelectStudents command.scheme />">
	<@listStudentIdInputs />
	<input type="hidden" name="filterQueryString" value="${command.filterQueryString!""}"/>
	<input type="hidden" name="returnTo" value="<@routes.manageEditSchemeStudents command.scheme.department command.scheme.academicYear.startYear?c command.scheme/>">
</form>

<script>
	jQuery(function($) {
		$('.fix-area').fixHeaderFooter();
	});
</script>

</#escape>