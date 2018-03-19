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
					Update student list from SITS
					<@fmt.help_popover id="linkToSits" content="You can no longer link to SITS for the current academic year, as changes for the forthcoming academic year are being made that will make the students on this scheme inaccurate." html=true/>
				<#else>
					<@f.checkbox path="findCommand.linkToSits" />
					Update student list from SITS
					<@fmt.help_popover id="linkToSits" content="<p>Select this option to update the list of students on this monitoring scheme as SITS data changes. For example, if a student withdraws from their course, they are removed from the scheme.</p><p>Deselect this option to keep the list updated manually.</p>" html=true/>
				</#if>
			</@bs3form.checkbox>

			<#if findCommand.linkToSits>
				<div class="alert alert-info hidden" id="unlinkSitsAlert">
					When you choose Save, any students added from SITS move to the list of manually added students. Changes to SITS data for these students will no longer
					synchronise with this scheme.
				</div>
				<script>
					$(function() {
						$('input[name=linkToSits]').on('change', function () {
							$('#unlinkSitsAlert').toggleClass('hidden', $(this).is(':checked'));
						});
					});
				</script>
			</#if>

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