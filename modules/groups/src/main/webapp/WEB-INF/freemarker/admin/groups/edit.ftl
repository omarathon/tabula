<#escape x as x?html>
<#import "*/group_components.ftl" as components />

<h1>Edit small groups</h1>
<h4><span class="muted">for</span> <@fmt.module_name module /></h4>

<#if smallGroupSet?? && smallGroupSet.allocationMethod.dbValue == 'StudentSignUp'><div class="alert">These groups are currently <strong>${smallGroupSet.openForSignups?string("open","closed")}</strong> for self sign-up</div></#if>

<@f.form method="post" action="${url('/groups/admin/module/${module.code}/groups/${smallGroupSet.id}/edit')}" commandName="editSmallGroupSetCommand" cssClass="form-horizontal">
	<@components.set_wizard false 'properties' smallGroupSet />

	<@f.errors cssClass="error form-errors" />
	
	<#assign newRecord=false />
	<#include "_fields.ftl" />
	
	<div class="submit-buttons">
		<input
			type="submit"
			class="btn btn-success"
			name="${ManageSmallGroupsMappingParameters.editAndAddGroups}"
			value="Add groups"
			/>
		<input
			type="submit"
			class="btn btn-primary"
			name="create"
			value="Save"
			/>
		<a class="btn" href="<@routes.depthome module=smallGroupSet.module />">Cancel</a>
	</div>

</@f.form>

</#escape>