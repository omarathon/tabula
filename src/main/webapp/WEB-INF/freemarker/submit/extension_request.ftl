<#escape x as x?html>
<#compress>
	<#assign time_remaining=durationFormatter(assignment.closeDate) />
	<h1>Request an extension for <strong>${assignment.name}</strong></h1><br/>
	<p>
		This assignment closes on  <@fmt.date date=assignment.closeDate timezone=true /> (${time_remaining} remaining).
		To request an extension for this assignment please read the Extension Guidelines below and submit this form.
		You will receive a notification when your application has been processed.
	</p><br/>
	<ul>
		<li>Summary of guidelines goes here</li>
		<li>Only really key points. Detail can be found using the link</li>
		<li>My dog ate my assignment is not a valid reason for an extension</li>
		<li>My dog ate my face is a valid reason for an extension</li>
	</ul><br/>
	<p>
	You should read the full <a href="">extension guidelines</a> before submitting your application for an extension.
	</p><br/>
	<@f.form method="post" class="form-horizontal" action="/module/${module.code}/${assignment.id}/extension" commandName="extensionRequestCommand">

		<@form.labelled_row "reason" "Please give a full statement of your reasons for applying for an extension">
			<@f.textarea path="reason" cssClass="text big-textarea" />
		</@form.labelled_row>

		<@form.labelled_row "requestedExpiryDate" "Until when do you request an extension?">
			<@f.input path="requestedExpiryDate" cssClass="date-time-picker" />
		</@form.labelled_row>

		<@form.row>
			<@form.label></@form.label>
			<@form.field>
				<label class="checkbox">
					<@f.checkbox path="readGuidelines" />
					I confirm that I have read the extension guidelines.
					</label>
					<@f.errors path="readGuidelines" cssClass="error" />
			</@form.field>
		</@form.row>

		<div class="submit-buttons">
			<input type="submit" class="btn btn-primary" value="Create" />
			or <a href=".." class="btn">Cancel</a>
		</div>

	</@f.form>
</#compress>
</#escape>