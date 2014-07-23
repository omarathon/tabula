<#escape x as x?html>
<#import "*/group_components.ftl" as components />

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

<h1>Edit reusable small groups: ${smallGroupSet.name}</h1>

<form method="POST">
	<input type="hidden" name="filterQueryString" value="${findCommand.serializeFilter}" />
	<@listStudentIdInputs />

	<p class="progress-arrows">
		<span class="arrow-right">Properties</span>
		<span class="arrow-right arrow-left active">Students</span>
		<span class="arrow-right arrow-left use-tooltip" title="Save and edit groups"><button type="submit" class="btn btn-link" name="${ManageDepartmentSmallGroupsMappingParameters.editAndAddGroups}">Groups</button></span>
		<span class="arrow-right arrow-left use-tooltip" title="Save and allocate students to groups"><button type="submit" class="btn btn-link" name="${ManageDepartmentSmallGroupsMappingParameters.editAndAllocate}">Allocate</button></span>
	</p>

	<div class="fix-area">
		<#include "_selectStudents.ftl" />

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
				class="btn btn-success use-tooltip"
				name="${ManageDepartmentSmallGroupsMappingParameters.editAndAddGroups}"
				value="Save and edit groups"
				title="Edit groups for set of reusable groups"
				data-container="body"
				/>
			<input
				type="submit"
				class="btn btn-primary spinnable spinner-auto"
				name="persist"
				value="Save and exit"
				/>
			<a class="btn" href="<@routes.crossmodulegroups smallGroupSet.department />">Cancel</a>
		</div>
	</div>
</form>

</#escape>