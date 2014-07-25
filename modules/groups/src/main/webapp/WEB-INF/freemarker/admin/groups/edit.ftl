<#escape x as x?html>

<h1>Edit small groups</h1>
<h4><span class="muted">for</span> <@fmt.module_name module /></h4>

<#if smallGroupSet?? && smallGroupSet.allocationMethod.dbValue == 'StudentSignUp'><div class="alert">These groups are currently <strong>${smallGroupSet.openForSignups?string("open","closed")}</strong> for self sign-up</div></#if>

<@f.form method="post" action="${url('/groups/admin/module/${module.code}/groups/${smallGroupSet.id}/edit')}" commandName="editSmallGroupSetCommand" cssClass="form-horizontal">
	<p class="progress-arrows">
		<span class="arrow-right active">Properties</span>
		<span class="arrow-right arrow-left use-tooltip" title="Save and edit students"><button type="submit" class="btn btn-link" name="${ManageSmallGroupsMappingParameters.editAndAddStudents}">Students</button></span>
		<span class="arrow-right arrow-left use-tooltip" title="Save and edit groups"><button type="submit" class="btn btn-link" name="${ManageSmallGroupsMappingParameters.editAndAddGroups}">Groups</button></span>
		<span class="arrow-right arrow-left use-tooltip" title="Save and edit events"><button type="submit" class="btn btn-link" name="${ManageSmallGroupsMappingParameters.editAndAddEvents}">Events</button></span>
		<span class="arrow-right arrow-left use-tooltip" title="Save and allocate students to groups"><button type="submit" class="btn btn-link" name="${ManageSmallGroupsMappingParameters.editAndAllocate}">Allocate</button></span>
	</p>

	<@f.errors cssClass="error form-errors" />
	
	<#assign newRecord=false />
	<#include "_fields.ftl" />
	
	<div class="submit-buttons">
		<input
			type="submit"
			class="btn btn-success"
			name="${ManageSmallGroupsMappingParameters.editAndAddStudents}"
			value="Save and add students"
			title="Add students to these groups"
			/>
		<input
			type="submit"
			class="btn btn-primary"
			name="create"
			value="Save and exit"
			title="Save your groups and add students later"
			/>
		<a class="btn" href="<@routes.depthome module=smallGroupSet.module />">Cancel</a>
	</div>

</@f.form>

</#escape>