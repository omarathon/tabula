<#escape x as x?html>
<#-- Field to support redirection post-submit -->
<input type="hidden" name="action" value="submit" id="action-submit">

	<@bs3form.labelled_form_group path="feedbackTemplate" labelText="Feedback template">
		<@f.select path="feedbackTemplate" id="feedbackTemplate" cssClass="form-control">
			<@f.option value="" label="None"/>
			<@f.options items=department.feedbackTemplates itemLabel="name" itemValue="id" />
		</@f.select>
    <div class="help-block"><a href="<@routes.cm2.feedbacktemplates department />">Create new feedback template</a></div>
	</@bs3form.labelled_form_group>

	<@bs3form.labelled_form_group path="automaticallyReleaseToMarkers" labelText="Automated submission release">
		<@bs3form.checkbox path="automaticallyReleaseToMarkers">
			<@f.checkbox path="automaticallyReleaseToMarkers" id="automaticallyReleaseToMarkers" />
				Automatically release to markers when assignment closes or after plagiarism check
		</@bs3form.checkbox>
		<div class="help-block">
			Students who do not submit work are not released automatically - you need to release them manually.
		</div>
	</@bs3form.labelled_form_group>

	<@bs3form.labelled_form_group path="collectMarks" labelText="Marks">
		<@bs3form.checkbox path="collectMarks">
			<@f.checkbox path="collectMarks" id="collectMarks" /> Collect marks
		</@bs3form.checkbox>
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

	<@bs3form.labelled_form_group path="dissertation" labelText="Dissertations">
		<@bs3form.checkbox path="dissertation">
			<@f.checkbox path="dissertation" id="dissertation" />
			This assignment is a dissertation
		</@bs3form.checkbox>
		<div class="help-block">
			Dissertations don't have a 20-day turnaround time for feedback.
		</div>
	</@bs3form.labelled_form_group>

	<@bs3form.labelled_form_group path="publishFeedback" labelText="Publish feedback to students">
		<@bs3form.checkbox path="publishFeedback">
			<@f.checkbox path="publishFeedback" id="publishFeedback" />
			Allow feedback to be published to students for this assignment
		</@bs3form.checkbox>
		<div class="help-block">
			If you don't check this box, you won't be able to publish any feedback to students.
		</div>
	</@bs3form.labelled_form_group>
</#escape>