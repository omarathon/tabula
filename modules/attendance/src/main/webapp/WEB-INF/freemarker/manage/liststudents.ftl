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

<h1>Create a scheme</h1>

<p class="progress-arrows">
	<span class="arrow-right">Properties</span>
	<span class="arrow-right arrow-left active">Students</span>
	<span class="arrow-right arrow-left">Points</span>
</p>

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

		<form method="POST" action="<@routes.manageSelectStudents command.scheme />">
			<@listStudentIdInputs />
			<input type="hidden" name="filterQueryString" value="${command.filterQueryString}"/>
			<input
				type="submit"
				class="btn"
				value="Select students for scheme"
			/>
		</form>

		<form id="newSchemeAddStudents" method="POST">
	<#else>
		<details>
			<summary class="large-chevron collapsible">
				<span class="legend">Students
					<small>Select which students this scheme should apply to</small>
				</span>

				<p><@fmt.p membershipItems?size "student" /> on this scheme</p>

				<form method="POST" action="<@routes.manageSelectStudents command.scheme />">
					<@listStudentIdInputs />
					<input type="hidden" name="filterQueryString" value="${command.filterQueryString}"/>
					<input
						type="submit"
						class="btn"
						value="Select students for scheme"
					/>
				</form>
			</summary>

			<@attendance_macros.manageStudentTable membershipItems />
		</details>

	</#if>

	<p>&nbsp;</p>

	<form id="newSchemeAddStudents" method="POST">
		<@listStudentIdInputs />
		<input
				type="submit"
				class="btn btn-success use-tooltip"
				name="${CreateSchemeMappingParameters.createAndAddPoints}"
				value="Add points"
				title="Select which monitoring points this scheme should use"
				data-container="body"
				/>
		<input
				type="submit"
				class="btn btn-primary use-tooltip"
				name="create"
				value="Save"
				title="Save your blank scheme and add points to it later"
				data-container="body"
				/>
		<a class="btn" href="<@routes.manageHomeForYear command.scheme.department command.scheme.academicYear.startYear?c />">Cancel</a>
	</form>

</#escape>