<#import "*/cm2_macros.ftl" as cm2 />
<#escape x as x?html>

	<@cm2.workflowHeader "Delete marking workflow" workflow "" />

	<p>Are you sure you want to permanently delete "<strong>${workflow.name}</strong>"?</p>

	<#assign formAction><@routes.cm2.reusableWorkflowDelete department academicYear workflow /></#assign>
	<@f.form
		method="POST"
		class="double-submit-protection"
		action="${formAction}"
		modelAttribute="deleteMarkingWorkflowCommand">

		<@bs3form.labelled_form_group>
			<input type="submit" value="Delete" class="btn btn-primary">
			<a class="btn btn-default" href="<@routes.cm2.reusableWorkflowsHome department academicYear />">Cancel</a>
		</@bs3form.labelled_form_group>

	</@f.form>
</#escape>