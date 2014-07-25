<#escape x as x?html>
<#import "*/group_components.ftl" as components />
	<h1>Edit small groups</h1>
	<h4><span class="muted">for</span> <@fmt.module_name module /></h4>

	<@f.form id="editGroups" method="POST" commandName="command" class="form-horizontal">
		<p class="progress-arrows">
			<span class="arrow-right">Properties</span>
			<#if smallGroupSet.linked>
				<span class="arrow-right arrow-left">Students</span>
				<span class="arrow-right arrow-left active">Groups</span>
				<span class="arrow-right arrow-left"><a href="<@routes.editsetevents smallGroupSet />">Events</a></span>
				<span class="arrow-right arrow-left"><a href="<@routes.editsetallocate smallGroupSet />">Allocate</a></span>
			<#else>
				<span class="arrow-right arrow-left use-tooltip" title="Save and edit students"><button type="submit" class="btn btn-link" name="${ManageSmallGroupsMappingParameters.editAndAddStudents}">Students</button></span>
				<span class="arrow-right arrow-left active">Groups</span>
				<span class="arrow-right arrow-left use-tooltip" title="Save and edit events"><button type="submit" class="btn btn-link" name="${ManageSmallGroupsMappingParameters.editAndAddEvents}">Events</button></span>
				<span class="arrow-right arrow-left use-tooltip" title="Save and allocate students to groups"><button type="submit" class="btn btn-link" name="${ManageSmallGroupsMappingParameters.editAndAllocate}">Allocate</button></span>
			</#if>
		</p>

		<#include "_editGroups.ftl" />

		<div class="submit-buttons">
			<#if smallGroupSet.linked>
				<a class="btn btn-success" href="<@routes.editsetevents smallGroupSet />">Add events</a>
			<#else>
				<input
					type="submit"
					class="btn btn-success"
					name="${ManageSmallGroupsMappingParameters.editAndAddEvents}"
					value="Save and add events"
					title="Add events to these groups"
					/>
				<input
					type="submit"
					class="btn btn-primary"
					name="create"
					value="Save and exit"
					title="Save your groups and add events later"
					/>
			</#if>
			<a class="btn" href="<@routes.depthome module=smallGroupSet.module />">Cancel</a>
		</div>
	</@f.form>
</#escape>