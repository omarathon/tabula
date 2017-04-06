<#escape x as x?html>
	<#-- Field to support redirection post-submit -->
	<input type="hidden" name="action" value="submit" id="action-submit">

	<@bs3form.labelled_form_group path="feedbackTemplate" labelText="Feedback template:">
		<div class="input-group">
			<@f.select path="feedbackTemplate" id="feedbackTemplate">
				<@f.option value="" label="No template"/>
				<@f.options items=department.feedbackTemplates itemLabel="name" itemValue="id" />
			</@f.select>
		</div>
		<div class="help-block"><a href="<@routes.cm2.feedbacktemplates department />">Create new feedback template</a></div>
	</@bs3form.labelled_form_group>

	<@bs3form.labelled_form_group path="automaticallyReleaseToMarkers" labelText="Automated submission release:">
		<div class="input-group">
			<@bs3form.checkbox path="automaticallyReleaseToMarkers">
				<@f.checkbox path="automaticallyReleaseToMarkers" id="automaticallyReleaseToMarkers" />
				Automatically release to markers when assignment closes or after plagiarism check
			</@bs3form.checkbox>
		</div>
	</@bs3form.labelled_form_group>

	<@bs3form.labelled_form_group path="collectMarks" labelText="Marks:">
		<div class="input-group">
			<@f.checkbox path="collectMarks" id="collectMarks" />
			Collect Marks
		</div>
	</@bs3form.labelled_form_group>

	<@bs3form.labelled_form_group labelText="Credit bearing">
		<@bs3form.radio>
			<@f.radiobutton path="summative" value="true" />
		Summative (counts towards final mark)
		</@bs3form.radio>
		<@bs3form.radio>
			<@f.radiobutton path="summative" value="false" />
		Formative (does not count towards final mark)
		</@bs3form.radio>
	</@bs3form.labelled_form_group>

	<@bs3form.labelled_form_group path="dissertation" labelText="Dissertation set:">
		<div class="input-group">
			<@f.checkbox path="dissertation" id="dissertation" />
			Set assignment as a dissertation
		</div>
	</@bs3form.labelled_form_group>
	<div class="help-block">Dissertations don't have a 20 day turnaround time for feedback.</div>

</#escape>