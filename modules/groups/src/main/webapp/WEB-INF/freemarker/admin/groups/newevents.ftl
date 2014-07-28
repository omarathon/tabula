<#escape x as x?html>
<#import "*/group_components.ftl" as components />
	<h1>Create small groups</h1>
	<h4><span class="muted">for</span> <@fmt.module_name module /></h4>

	<@f.form id="newEvents" method="POST" commandName="command" class="form-horizontal">
		<p class="progress-arrows">
			<span class="arrow-right">Properties</span>
			<span class="arrow-right arrow-left">Students</span>
			<span class="arrow-right arrow-left">Groups</span>
			<span class="arrow-right arrow-left active">Events</span>
			<span class="arrow-right arrow-left use-tooltip" title="Save and allocate students to groups"><button type="submit" class="btn btn-link" name="${ManageSmallGroupsMappingParameters.createAndAllocate}">Allocate</button></span>
		</p>

		<#include "_editEvents.ftl" />

		<div class="submit-buttons">
			<input
				type="submit"
				class="btn btn-success"
				name="${ManageSmallGroupsMappingParameters.createAndAllocate}"
				value="Save and allocate students"
				title="Add students to these groups"
				/>
			<input
				type="submit"
				class="btn btn-primary"
				name="create"
				value="Save and exit"
				title="Save your groups and allocate students later"
				/>
			<a class="btn" href="<@routes.depthome module=smallGroupSet.module />">Cancel</a>
		</div>
	</@f.form>
</#escape>