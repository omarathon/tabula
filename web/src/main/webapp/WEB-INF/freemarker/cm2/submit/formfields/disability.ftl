<#if features.disabilityOnSubmission>
	<@bs3form.labelled_form_group "useDisability">
		<@bs3form.label path="useDisability" for="useDisability">
			Show disability
			<i class="text-primary fa fa-info-circle use-popover" data-html="true" data-content="<p>You have self-reported the following disability code:</p><div class='well'><h6>${disability.code}</h6>${(disability.sitsDefinition)!}</div>"></i>
		</@bs3form.label>
		<@form.field>
			<@f.select id="useDisability" path="useDisability">
				<option disabled value="" <#if !submitAssignmentCommand.useDisability??>selected</#if>></option>
				<option value="true" <#if submitAssignmentCommand.useDisability?? && submitAssignmentCommand.useDisability>selected</#if>>Yes</option>
				<option value="false" <#if submitAssignmentCommand.useDisability?? && !submitAssignmentCommand.useDisability>selected</#if>>No</option>
			</@f.select>
		</@form.field>
		<div class="help-block">
			Choose whether the marker should be made aware of your ${disability.definition?lower_case?replace('impaired', 'impairment')}.
			They make take this into consideration when marking.
		</div>
	</@bs3form.labelled_form_group>
</#if>