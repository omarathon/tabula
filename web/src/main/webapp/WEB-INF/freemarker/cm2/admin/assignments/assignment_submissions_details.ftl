<#import "*/assignment_components.ftl" as components />
<#import "*/cm2_macros.ftl" as cm2 />
<#escape x as x?html>

	<#if mode == 'new'>
		<#assign assignmentEditMode=false />
	<#else>
		<#assign assignmentEditMode=true />
	</#if>

	<@cm2.assignmentHeader "Edit submission settings" assignment "for" />

<div class="fix-area">
	<#assign actionUrl><@routes.cm2.assignmentsubmissions assignment mode/></#assign>
	<@f.form method="post" action=actionUrl cssClass="dirty-check double-submit-protection">
		<@components.assignment_wizard 'submissions' assignment.module assignmentEditMode assignment />
		<#assign department = assignment.module.adminDepartment />
		<#include "_submissions_fields.ftl" />
        <div class="fix-footer">
			<#if mode == 'new'>
                <input
                        type="submit"
                        class="btn btn-primary"
                        name="${ManageAssignmentMappingParameters.createAndAddOptions}"
                        value="Save and continue"
                />
                <input
                        type="submit"
                        class="btn btn-primary"
                        name="${ManageAssignmentMappingParameters.createAndAddSubmissions}"
                        value="Save and exit"
                />
			<#else>
                <input
                        type="submit"
                        class="btn btn-primary"
                        name="${ManageAssignmentMappingParameters.editAndAddOptions}"
                        value="Save and continue"
                />
                <input
                        type="submit"
                        class="btn btn-primary"
                        name="${ManageAssignmentMappingParameters.editAndAddSubmissions}"
                        value="Save and exit"
                />
			</#if>
        </div>
	</@f.form>
</div>
</#escape>