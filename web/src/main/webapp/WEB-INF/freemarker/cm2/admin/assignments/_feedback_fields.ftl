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

	<@bs3form.labelled_form_group path="automaticallyReleaseToMarkers" labelText="Automated release for marking">
		<@bs3form.checkbox path="automaticallyReleaseToMarkers">
			<@f.checkbox path="automaticallyReleaseToMarkers" id="automaticallyReleaseToMarkers" />
			Automatically release assignment to markers on the assignment closing date
		</@bs3form.checkbox>
		<div class="help-block">
			Markers are notified of all students allocated to them, including students who have not submitted work.
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

	<@bs3form.labelled_form_group path="publishFeedback" labelText="Publish feedback to students">
		<@bs3form.checkbox path="publishFeedback">
			<@f.checkbox path="publishFeedback" id="publishFeedback" />
			Allow feedback to be published to students for this assignment
		</@bs3form.checkbox>
		<div class="help-block">
			Deselecting this option will allow feedback to be added to submissions but not published to students. Ideal for exam related assignments
		</div>
	</@bs3form.labelled_form_group>

	<fieldset id="dissertation-checkbox" <#if !command.publishFeedback>disabled</#if>>
		<@bs3form.labelled_form_group path="dissertation" labelText="Feedback turnaround time">
			<@bs3form.checkbox path="dissertation">
				<@f.checkbox path="dissertation" id="dissertation" />
				This assignment is exempt from the
				<a href="https://warwick.ac.uk/services/aro/dar/quality/categories/examinations/assessmentstrat/assessment/timeliness/" target="_blank">universal requirement</a>
				to return feedback within 20 University working days
			</@bs3form.checkbox>
		</@bs3form.labelled_form_group>
	</fieldset>
</#escape>