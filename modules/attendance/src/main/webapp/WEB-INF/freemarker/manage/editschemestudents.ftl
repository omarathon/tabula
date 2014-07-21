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

<h1>Edit scheme: ${scheme.displayName}</h1>

<form method="POST">
	<input type="hidden" name="filterQueryString" value="${findCommand.serializeFilter}" />
	<@listStudentIdInputs />

	<p class="progress-arrows">
		<span class="arrow-right use-tooltip" title="Save and edit properties"><button type="submit" class="btn btn-link" name="${ManageSchemeMappingParameters.saveAndEditProperties}">Properties</button></span>
		<span class="arrow-right arrow-left use-tooltip active">Students</span>
		<span class="arrow-right arrow-left use-tooltip" title="Save and edit points"><button type="submit" class="btn btn-link" name="${ManageSchemeMappingParameters.createAndAddPoints}">Points</button></span>
	</p>

	<div class="fix-area">

		<#include "_selectstudents.ftl" />

		<div class="fix-footer submit-buttons">
			<p style="padding-left: 20px;">
				<label>
					<@f.checkbox path="findCommand.linkToSits" />
					Link to SITS
					<#assign popoverContent><#noescape>
						If ticked, this filter will be automatically update this group of students from SITS.
						<br />
						If not, these students will be imported into a static list which will <strong>not</strong> be updated from SITS.
					</#noescape></#assign>
					<a class="use-popover"
					   id="popover-linkToSits"
					   data-content="${popoverContent}"
					   data-html="true"
							>
						<i class="icon-question-sign"></i>
					</a>
				</label>
			</p>

			<input
				type="submit"
				class="btn btn-primary"
				name="persist"
				value="Save"
			/>
			<a class="btn" href="<@routes.manageHomeForYear scheme.department scheme.academicYear.startYear?c />">Cancel</a>
		</div>

	</div>

</form>

</#escape>