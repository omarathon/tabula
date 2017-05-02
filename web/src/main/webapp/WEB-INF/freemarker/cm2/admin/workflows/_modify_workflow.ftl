<#assign cancelAction><@routes.cm2.reusableWorkflowsHome department academicYear /></#assign>

<#function route_function dept>
	<#local selectCourseCommand><@routes.cm2.reusableWorkflowsHome dept academicYear /></#local>
	<#return selectCourseCommand />
</#function>

<@fmt.id7_deptheader title="Define marking workflow" route_function=route_function preposition="for" />

<@f.form
	method="POST"
	class="double-submit-protection"
	action="${formAction}"
	commandName="${commandName}"
	>
		<@f.errors cssClass="error form-errors" />

		<@bs3form.labelled_form_group path="workflowName" labelText="Name" help="A descriptive name that will be used to refer to this marking workflow elsewhere.">
			<@f.input type="text" path="workflowName" cssClass="form-control" maxlength="255" placeholder="" />
		</@bs3form.labelled_form_group>

		<#include "_shared_fields.ftl" />

		<@bs3form.labelled_form_group>
			<input type="submit" value="Save" class="btn btn-primary">
			<a class="btn btn-default" href="${cancelAction}">Cancel</a>
		</@bs3form.labelled_form_group>

</@f.form>