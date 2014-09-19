<#escape x as x?html>
<#import "*/group_components.ftl" as components />
	<h1>Create small groups</h1>
	<h4><span class="muted">for</span> <@fmt.module_name module /></h4>

	<@f.form id="newEvents" method="POST" commandName="command" class="form-horizontal">
		<@components.set_wizard true 'events' smallGroupSet />

		<#assign is_edit = false />
		<#include "_editEvents.ftl" />

		<div class="submit-buttons">
			<input
				type="submit"
				class="btn btn-success"
				name="${ManageSmallGroupsMappingParameters.createAndAllocate}"
				value="Allocate students"
				/>
			<input
				type="submit"
				class="btn btn-primary"
				name="create"
				value="Save"
				/>
			<a class="btn" href="<@routes.depthome module=smallGroupSet.module academicYear=smallGroupSet.academicYear/>">Cancel</a>
		</div>
	</@f.form>
</#escape>