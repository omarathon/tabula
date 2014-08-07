<#escape x as x?html>
	<#import "*/group_components.ftl" as components />

	<h1>Set default event properties</h1>
	<h4><span class="muted">for</span> ${smallGroupSet.name}</h4>

	<@f.form method="post" action="" commandName="command" cssClass="form-horizontal">
		<@f.errors cssClass="error form-errors" />

		<fieldset>
			<@form.labelled_row "defaultTutors" "Tutors">
				<@form.flexipicker path="defaultTutors" placeholder="User name" list=true multiple=true auto_multiple=false />
			</@form.labelled_row>

			<@components.week_selector "defaultWeeks" allTerms smallGroupSet />

			<@form.labelled_row "defaultLocation" "Location">
				<@f.hidden path="defaultLocationId" />
				<@f.input path="defaultLocation" />
			</@form.labelled_row>
		</fieldset>

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