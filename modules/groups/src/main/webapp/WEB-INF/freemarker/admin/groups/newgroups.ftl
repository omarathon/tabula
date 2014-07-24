<#escape x as x?html>
<#import "*/group_components.ftl" as components />
	<h1>Create small groups</h1>
	<h4><span class="muted">for</span> <@fmt.module_name module /></h4>

	<@f.form id="newGroups" method="POST" commandName="command" class="form-horizontal">
		<p class="progress-arrows">
			<span class="arrow-right">Properties</span>
			<span class="arrow-right arrow-left">Students</span>
			<span class="arrow-right arrow-left active">Groups</span>
			<span class="arrow-right arrow-left use-tooltip" title="Save and edit events">Events</span>
			<span class="arrow-right arrow-left use-tooltip" title="Save and allocate students to groups">Allocate</span>
		</p>

		<#include "_editGroups.ftl" />

		<div class="submit-buttons">
			<input
				type="submit"
				class="btn btn-success use-tooltip"
				name="${ManageSmallGroupsMappingParameters.createAndAddEvents}"
				value="Save and add events"
				title="Add events to these groups"
				data-container="body"
				/>
			<input
				type="submit"
				class="btn btn-primary use-tooltip"
				name="create"
				value="Save and exit"
				title="Save your groups and add events later"
				data-container="body"
				/>
			<a class="btn" href="<@routes.depthome module=smallGroupSet.module />">Cancel</a>
		</div>
	</@f.form>
</#escape>