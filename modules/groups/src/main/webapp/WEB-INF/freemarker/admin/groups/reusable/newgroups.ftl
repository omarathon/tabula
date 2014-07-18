<#escape x as x?html>
<#import "*/group_components.ftl" as components />
	<h1>Create a set of reusable small groups</h1>

	<@f.form id="newGroups" method="POST" commandName="command" class="form-horizontal">
		<p class="progress-arrows">
			<span class="arrow-right">Properties</span>
			<span class="arrow-right arrow-left">Students</span>
			<span class="arrow-right arrow-left active">Groups</span>
			<span class="arrow-right arrow-left use-tooltip" title="Save and allocate students to groups"><button type="submit" class="btn btn-link" name="${ManageDepartmentSmallGroupsMappingParameters.createAndAllocate}">Allocate</button></span>
		</p>

		<div class="submit-buttons">
			<input
				type="submit"
				class="btn btn-success use-tooltip"
				name="${ManageDepartmentSmallGroupsMappingParameters.createAndAllocate}"
				value="Save and allocate students to groups"
				title="Allocate students to this set of reusable groups"
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