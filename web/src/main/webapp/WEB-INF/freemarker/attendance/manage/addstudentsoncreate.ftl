<#escape x as x?html>
<#import "../attendance_macros.ftl" as attendance_macros />

<#macro listStudentIdInputs>
	<#list findCommand.staticStudentIds as id>
		<input type="hidden" name="staticStudentIds" value="${id}" />
	</#list>
	<#list findCommand.includedStudentIds as id>
		<input type="hidden" name="includedStudentIds" value="${id}" />
	</#list>
	<#list findCommand.excludedStudentIds as id>
		<input type="hidden" name="excludedStudentIds" value="${id}" />
	</#list>
</#macro>

<h1>Create scheme: ${scheme.displayName}</h1>

<form class="add-student-to-scheme" method="POST">
	<input type="hidden" name="filterQueryString" value="${findCommand.serializeFilter}" />
	<@listStudentIdInputs />

	<p class="progress-arrows">
		<span class="arrow-right">Properties</span>
		<span class="arrow-right arrow-left active">Students</span>
		<span class="arrow-right arrow-left use-tooltip" title="Save and edit points"><button type="submit" class="btn btn-link" name="${ManageSchemeMappingParameters.createAndAddPoints}">Points</button></span>
	</p>

	<div class="fix-area">

		<#include "_selectstudents.ftl" />

		<div class="fix-footer submit-buttons">
			<@bs3form.checkbox path="findCommand.linkToSits">
				<#if SITSInFlux>
					<input type="checkbox" name="_linkToSits" value="on" disabled />
					Link to SITS
					<@fmt.help_popover id="linkToSits" content="You can no longer link to SITS for the current academic year, as changes for the forthcoming academic year are being made that will make the students on this scheme inaccurate." html=true/>
				<#else>
					<@f.checkbox path="findCommand.linkToSits" />
					Link to SITS
					<@fmt.help_popover id="linkToSits" content="<p>Select this option to automatically update the filtered list of students from SITS. If you choose not to link to SITS, these students are imported to Tabula as a static list, which does not update when SITS data changes. Therefore, you need to maintain the list yourself &ndash; e.g. when a student withdraws from their course.</p>
					<p>You can still upload missed monitoring points to SITS regardless of whether you select this option or not.</p>" html=true/>
				</#if>
			</@bs3form.checkbox>

			<@bs3form.form_group>
				<input
					type="submit"
					class="btn btn-primary use-tooltip spinnable spinner-auto"
					name="${ManageSchemeMappingParameters.createAndAddPoints}"
					value="Save and add points"
					title="Select which monitoring points this scheme should use"
					data-container="body"
					data-loading-text="Saving&hellip;"
				/>
				<input
					type="submit"
					class="btn btn-primary use-tooltip spinnable spinner-auto"
					name="persist"
					value="Save and exit"
					title="Save your blank scheme and add points to it later"
					data-container="body"
					data-loading-text="Saving&hellip;"
				/>
				<a class="btn btn-default" href="<@routes.attendance.manageHomeForYear scheme.department scheme.academicYear />">Cancel</a>
			</@bs3form.form_group>
		</div>

	</div>

</form>

</#escape>