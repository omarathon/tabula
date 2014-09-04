<#escape x as x?html>
<#import "*/group_components.ftl" as components />
	<h1>Create a set of reusable small groups</h1>

	<@f.form id="newGroups" method="POST" commandName="command" class="form-horizontal">
		<@components.reusable_set_wizard true 'groups' smallGroupSet />

		<#include "_editGroups.ftl" />

		<div class="submit-buttons">
			<input
				type="submit"
				class="btn btn-success use-tooltip"
				name="${ManageDepartmentSmallGroupsMappingParameters.createAndAddStudents}"
				value="Save and add students"
				title="Select which students are included in these groups"
				data-container="body"
				/>
			<input
				type="submit"
				class="btn btn-primary use-tooltip"
				name="create"
				value="Save and exit"
				title="Save your groups and allocate students later"
				data-container="body"
				/>
			<a class="btn" href="<@routes.crossmodulegroups smallGroupSet.department />">Cancel</a>
		</div>
	</@f.form>
</#escape>