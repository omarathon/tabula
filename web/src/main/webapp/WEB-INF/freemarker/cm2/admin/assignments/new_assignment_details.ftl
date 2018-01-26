<#escape x as x?html>
<#import "*/assignment_components.ftl" as components />
<#import "*/cm2_macros.ftl" as cm2 />
	<#--
	HFC-166 Don't use #compress on this file because
	the comments textarea needs to maintain newlines.
	-->

	<@cm2.moduleHeader "Create a new assignment" module "for" />

<div class="fix-area">
	<#assign actionUrl><@routes.cm2.createassignmentdetails module academicYear /></#assign>
	<@f.form cssClass="double-submit-protection" method="post" action=actionUrl>
		<#if !features.redirectCM1>
			<#include "_oldversion_redirect_banner.ftl" />
		</#if>
		<@components.assignment_wizard 'details' module />
		<#if command.prefilled>
			<div class="alert alert-info">
				<i class="fa fa-info-circle"></i>
				Some fields have been pre-filled from another recently created assignment for convenience.
				<a href='${actionUrl}?prefillFromRecent=false'>Don't do this</a>
			</div>
		</#if>
		<#if command.prefillAssignment??>
			<div class="alert alert-info">
				<i class="fa fa-question-circle"></i>
				Some fields have been pre-filled from assignment ${command.prefillAssignment.name}.
			</div>
		</#if>
		<@f.errors cssClass="error form-errors" />
		<#assign newRecord=true />
		<#assign canEditWorkflowType=true />
		<#include "_fields.ftl" />

		<#include "_modify_assignment_workflow.ftl" />

		<div class="fix-footer">
			<input
				type="submit"
				class="btn btn-primary"
				name="${ManageAssignmentMappingParameters.createAndAddFeedback}"
				value="Save and continue"
			/>
			<input
				type="submit"
				class="btn btn-primary"
				name="${ManageAssignmentMappingParameters.createAndAddDetails}"
				value="Save and exit"
			/>
		</div>
	</@f.form>
</div>
</#escape>