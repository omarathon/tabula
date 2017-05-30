<#escape x as x?html>
	<#import "*/assignment_components.ftl" as components />
	<#import "*/cm2_macros.ftl" as cm2 />

	<#if mode == 'new'>
		<#assign assignmentEditMode=false />
	<#else>
		<#assign assignmentEditMode=true />
	</#if>

	<@cm2.assignmentHeader "Edit students" assignment "for" />

<div class="fix-area assignment-student-details">
	<#assign actionUrl><@routes.cm2.assignmentstudents assignment mode/></#assign>
	<@f.form method="post" action=actionUrl cssClass="dirty-check">
		<@components.assignment_wizard 'students' assignment.module assignmentEditMode assignment />
		<@f.errors cssClass="error form-errors" />
        <div>
			<#include "_student_fields.ftl" />
        </div>
        <div class="fix-footer">
			<#if mode == 'new'>
                <input
                        type="submit"
                        class="btn btn-primary"
                        name="${ManageAssignmentMappingParameters.createAndAddMarkers}"
                        value="Save and continue"
                />
                <input
                        type="submit"
                        class="btn btn-primary"
                        name="${ManageAssignmentMappingParameters.createAndAddStudents}"
                        value="Save and exit"
                />
			<#else>
                <input
                        type="submit"
                        class="btn btn-primary"
                        name="${ManageAssignmentMappingParameters.editAndAddMarkers}"
                        value="Save and continue"
                />
                <input
                        type="submit"
                        class="btn btn-primary"
                        name="${ManageAssignmentMappingParameters.editAndAddStudents}"
                        value="Save and exit"
                />
			</#if>
        </div>
	</@f.form>
</div>
</#escape>