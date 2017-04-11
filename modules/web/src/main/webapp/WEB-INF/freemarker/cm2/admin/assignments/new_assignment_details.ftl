<#escape x as x?html>
<#import "*/assignment_components.ftl" as components />
	<#--
	HFC-166 Don't use #compress on this file because
	the comments textarea needs to maintain newlines.
	-->

<div class="deptheader">
	<h1>Create assignment</h1>
	<h4 class="with-related"><span class="muted">for</span> <@fmt.module_name module /></h4>
</div>
<div class="fix-area">
	<#assign actionUrl><@routes.cm2.createassignmentdetails module /></#assign>
	<@f.form method="post" action=actionUrl>
		<@components.assignment_wizard 'details' module />
		<#if command.prefilled>
			<div class="alert alert-info">
				<i class="icon-info-sign fa fa-info-circle"></i>
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
		<#include "_fields.ftl" />

		<div class="reusable-workflow-picker workflow-fields">
			<@bs3form.labelled_form_group path="reusableWorkflow" labelText="Workflow name">
				<@f.select path="reusableWorkflow" class="form-control" >
					<option value="" <#if !status.value??>selected</#if> disabled></option>
					<@f.options items=reusableWorkflows itemValue="id" itemLabel="name" />
				</@f.select>
			</@bs3form.labelled_form_group>
		</div>

		<div class="single-use-workflow-fields workflow-fields">
			<#assign isNew = true />
			<#include "../workflows/_shared_fields.ftl" />
		</div>

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
<script type="text/javascript">
	(function ($) { "use strict";

		$('select[name=workflowCategory]').on('change', function() {
			var $this = $(this);
			var value = $this.val();
			$('.workflow-fields').hide();
			if(value === "R") {
				$('.reusable-workflow-picker').show();
			} else if (value === "S") {
				$('.single-use-workflow-fields').show()
			}
		}).trigger('change');

	})(jQuery);
</script>
</#escape>