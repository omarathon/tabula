<#escape x as x?html>
	<#import "*/group_components.ftl" as components />

	<h1>Edit event</h1>
	<h4><span class="muted">for</span> ${smallGroup.name}</h4>

	<@f.form method="post" action="" commandName="editSmallGroupEventCommand" cssClass="form-horizontal">
		<@f.errors cssClass="error form-errors" />

		<#assign newRecord=false />
		<#include "_event_fields.ftl" />

		<div class="submit-buttons">
			<input
				type="submit"
				class="btn btn-primary"
				name="create"
				value="Save"
				/>
			<a class="btn" href="${cancelUrl}">Cancel</a>
		</div>
	</@f.form>
</#escape>