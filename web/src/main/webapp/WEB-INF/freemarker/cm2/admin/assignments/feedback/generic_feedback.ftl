<#escape x as x?html>
	<#assign submit_url><@routes.cm2.genericfeedback assignment /></#assign>
	<#if !ajax>
		<div class="deptheader">
			<h1>Generic feedback</h1>
			<h4><span class="muted">for</span> ${assignment.name}</h4>
		</div>
	</#if>

	<@f.form  method="post" commandName="command" action="${submit_url}">
		<@bs3form.labelled_form_group>
			<@f.textarea path="genericFeedback" cssClass="form-control text big-textarea" maxlength=4000 />
		</@bs3form.labelled_form_group>
		<div class="help-block">
			The following comments will be released to all students along with their individual feedback.
		</div>
		<div class="submit-buttons">
			<input class="before-save btn btn-primary" type="submit" value="Save">
			<a href="<@routes.cm2.assignmentsubmissionsandfeedbacksummary assignment />" class="btn btn-default">Cancel</a>
		</div>
	</@f.form>
</#escape>
