<#import "*/cm2_macros.ftl" as cm2 />

<#escape x as x?html>
	<@cm2.assignmentHeader "Delete submissions and/or feedback" assignment "for" />

	<#assign submitUrl><@routes.cm2.deleteSubmissionsAndFeedback assignment /></#assign>
	<@f.form method="post" action=submitUrl commandName="command">
		<@bs3form.errors path="" />
		<@f.hidden name="confirmScreen" value="true" />

		<p>Deleting submissions and feedback for <strong><@fmt.p command.students?size "student" /></strong>:</p>

		<ul>
			<@userlookup ids=command.students>
				<#list returned_users?keys?sort as id>
					<li>
						<#assign returned_user=returned_users[id] />
						<#if returned_user.foundUser>
							${returned_user.fullName} (<#if returned_user.warwickId??>${returned_user.warwickId}<#else>${id}</#if>)
						<#else>
							${id}
						</#if>
						<@f.hidden name="students" value=id />
					</li>
				</#list>
			</@userlookup>
		</ul>

		<@bs3form.labelled_form_group path="submissionOrFeedback" labelText="Please specify what you would like to delete:">
			<@bs3form.radio>
				<@f.radiobutton path="submissionOrFeedback" value="submissionOnly" /> Submissions only
			</@bs3form.radio>
			<@bs3form.radio>
				<@f.radiobutton path="submissionOrFeedback" value="feedbackOnly" /> Feedback only
			</@bs3form.radio>
			<@bs3form.radio>
				<@f.radiobutton path="submissionOrFeedback" value="submissionAndFeedback" /> Both submissions and feedback
			</@bs3form.radio>
		</@bs3form.labelled_form_group>

		<p>
			You only need to do this if if an erroneous submission has been made or the wrong feedback has been uploaded.
			If you are trying to re-use this assignment, you should go back and create a separate assignment instead.
		</p>

		<#if assignment.cm2Assignment && assignment.cm2MarkingWorkflow??>
			<p>
				<strong>Warning:</strong>
				If you delete feedback for students who have been allocated to a marker and/or released for marking, all in-progress marking will be permanently deleted and the students will
				need to be allocated to markers and released for marking again.
			</p>
		</#if>

		<@bs3form.form_group path="confirm">
			<@bs3form.checkbox path="confirm">
				<@f.checkbox path="confirm" /> I confirm that I want to permanently delete these submissions/feedback items.
			</@bs3form.checkbox>
		</@bs3form.form_group>

		<div class="submit-buttons">
			<button type="submit" class="btn btn-danger">Delete</button>
			<a class="btn btn-default" href="<@routes.cm2.assignmentsubmissionsandfeedback assignment />">Cancel</a>
		</div>

	</@f.form>
</#escape>