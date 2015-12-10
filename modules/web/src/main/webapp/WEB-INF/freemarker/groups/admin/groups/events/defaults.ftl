<#escape x as x?html>
	<#import "*/group_components.ftl" as components />

	<div class="deptheader">
		<h1>Set default event properties</h1>
		<h4 class="with-related"><span class="muted">for</span> ${smallGroupSet.name}</h4>
	</div>

	<p>Defaults set here will apply to any new events. You can adjust these properties for individual events.</p>

	<@f.form method="post" action="" commandName="command">
		<@f.errors cssClass="error form-errors" />
	
		<@bs3form.labelled_form_group path="defaultTutors" labelText="Tutors">
			<@form.flexipicker path="defaultTutors" placeholder="User name" list=true multiple=true auto_multiple=false />
		</@bs3form.labelled_form_group>

		<@components.week_selector "defaultWeeks" allTerms smallGroupSet />

		<@bs3form.labelled_form_group path="defaultLocation" labelText="Location">
			<@f.hidden path="defaultLocationId" />
			<@f.input path="defaultLocation" cssClass="form-control" />
		</@bs3form.labelled_form_group>

		<@bs3form.checkbox "resetExistingEvents">
			<@f.checkbox path="resetExistingEvents" value="true" />
			Reset existing events to these defaults
		</@bs3form.checkbox>

		<div class="submit-buttons">
			<input
				type="submit"
				class="btn btn-primary"
				name="create"
				value="Save"
			/>
			<a class="btn btn-default" href="${cancelUrl}">Cancel</a>
		</div>
	</@f.form>
</#escape>