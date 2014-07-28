<#escape x as x?html>
<#import "*/group_components.ftl" as components />
	<h1>Edit small groups</h1>
	<h4><span class="muted">for</span> <@fmt.module_name module /></h4>

	<@f.form id="editEvents" method="POST" commandName="command" class="form-horizontal">
		<p class="progress-arrows">
			<span class="arrow-right">Properties</span>
			<#if smallGroupSet.linked>
				<span class="arrow-right arrow-left">Students</span>
				<span class="arrow-right arrow-left">Groups</span>
			<#else>
				<span class="arrow-right arrow-left use-tooltip" title="Save and edit students"><button type="submit" class="btn btn-link" name="${ManageSmallGroupsMappingParameters.editAndAddStudents}">Students</button></span>
				<span class="arrow-right arrow-left use-tooltip" title="Save and edit groups"><button type="submit" class="btn btn-link" name="${ManageSmallGroupsMappingParameters.editAndAddGroups}">Groups</button></span>
			</#if>
			<span class="arrow-right arrow-left active">Events</span>
			<span class="arrow-right arrow-left use-tooltip" title="Save and allocate students to groups"><button type="submit" class="btn btn-link" name="${ManageSmallGroupsMappingParameters.editAndAllocate}">Allocate</button></span>
		</p>

		<#include "_editEvents.ftl" />

		<div class="submit-buttons">
			<input
				type="submit"
				class="btn btn-success"
				name="${ManageSmallGroupsMappingParameters.editAndAllocate}"
				value="Save and allocate students"
				title="Add students to these groups"
				/>
			<input
				type="submit"
				class="btn btn-primary"
				name="create"
				value="Save and exit"
				title="Save your groups and add events later"
				/>
			<a class="btn" href="<@routes.depthome module=smallGroupSet.module />">Cancel</a>
		</div>
	</@f.form>
</#escape>