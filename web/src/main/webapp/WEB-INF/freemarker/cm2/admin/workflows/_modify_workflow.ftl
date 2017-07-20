<#assign cancelAction><@routes.cm2.reusableWorkflowsHome department academicYear /></#assign>

<#function route_function dept>
	<#local selectCourseCommand><@routes.cm2.reusableWorkflowsHome dept academicYear /></#local>
	<#return selectCourseCommand />
</#function>
<@cm2.departmentHeader "Define marking workflow" department route_function academicYear />

<@f.form
	method="POST"
	class="double-submit-protection"
	action="${formAction}"
	commandName="${commandName}"
	>
		<@f.errors cssClass="error form-errors" />

		<@bs3form.labelled_form_group path="workflowName" labelText="Name" help="Use a descriptive name as this appears throughout Tabula to refer to this marking workflow.">
			<@f.input type="text" path="workflowName" cssClass="form-control" maxlength="255" placeholder="" />
		</@bs3form.labelled_form_group>

		<#include "_shared_fields.ftl" />

		<@bs3form.labelled_form_group>
			<input type="submit" value="Save" class="btn btn-primary">
			<a class="btn btn-default" href="${cancelAction}">Cancel</a>
		</@bs3form.labelled_form_group>

</@f.form>