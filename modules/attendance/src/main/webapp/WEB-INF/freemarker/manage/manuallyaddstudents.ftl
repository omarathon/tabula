<#escape x as x?html>

<h1>Add students manually</h1>

<form action="" method="POST" class="mass-add-users">

	<input type="hidden" name="filterQueryString" value="${findCommand.filterQueryString}">
	<#list findCommand.staticStudentIds as id>
		<input type="hidden" name="staticStudentIds" value="${id}" />
	</#list>
	<#list findCommand.updatedStaticStudentIds as id>
		<input type="hidden" name="updatedStaticStudentIds" value="${id}" />
	</#list>
	<#list editMembershipCommand.includedStudentIds as id>
		<input type="hidden" name="includedStudentIds" value="${id}" />
	</#list>
	<#list editMembershipCommand.updatedIncludedStudentIds as id>
		<input type="hidden" name="updatedIncludedStudentIds" value="${id}" />
	</#list>
	<#list editMembershipCommand.excludedStudentIds as id>
		<input type="hidden" name="excludedStudentIds" value="${id}" />
	</#list>
	<#list editMembershipCommand.updatedExcludedStudentIds as id>
		<input type="hidden" name="updatedExcludedStudentIds" value="${id}" />
	</#list>

	<p>Type or paste in a list of usercodes or University numbers here, separated by white space, then click <code>Add</code>.</p>

	<textarea rows="6" class="input-block-level" name="massAddUsers"></textarea>

	<input
		type="submit"
		class="btn btn-success disabled spinnable spinner-auto add-students"
		name="${manuallyAddSubmitString}"
		value="Add"
	/>

</form>

</#escape>