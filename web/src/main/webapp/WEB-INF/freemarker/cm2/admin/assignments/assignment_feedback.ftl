<#escape x as x?html>
	<#import "*/assignment_components.ftl" as components />
	<#import "*/cm2_macros.ftl" as cm2 />

	<#if mode == 'new'>
		<#assign assignmentEditMode=false />
	<#else>
		<#assign assignmentEditMode=true />
	</#if>

	<@cm2.assignmentHeader "Edit feedback settings" assignment "for" />

<div class="fix-area">
	<#assign actionUrl><@routes.cm2.assignmentfeedback assignment mode /></#assign>
	<@f.form method="post" action=actionUrl  cssClass="dirty-check double-submit-protection">
		<@components.assignment_wizard 'feedback' assignment.module assignmentEditMode assignment />
		<@f.errors cssClass="error form-errors" />
		<#assign newRecord=false />
		<#include "_feedback_fields.ftl" />

        <div class="fix-footer">
			<#if mode == 'new'>
                <input
                        type="submit"
                        class="btn btn-primary"
                        name="${ManageAssignmentMappingParameters.createAndAddStudents}"
                        value="Save and continue"
                />
                <input
                        type="submit"
                        class="btn btn-primary"
                        name="${ManageAssignmentMappingParameters.createAndAddFeedback}"
                        value="Save and exit"
                />
			<#else>
                <input
                        type="submit"
                        class="btn btn-primary"
                        name="${ManageAssignmentMappingParameters.editAndAddStudents}"
                        value="Save and continue"
                />
                <input
                        type="submit"
                        class="btn btn-primary"
                        name="${ManageAssignmentMappingParameters.editAndAddFeedback}"
                        value="Save and exit"
                />
			</#if>
        </div>
	</@f.form>
</div>
</#escape>