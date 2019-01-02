<#import "*/cm2_macros.ftl" as cm2 />
<#escape x as x?html>
	<#assign submit_url><@routes.cm2.genericfeedback assignment /></#assign>
	<#if !ajax>
		<@cm2.assignmentHeader "Generic feedback" assignment "for" />
	</#if>

	<@f.form  method="post" modelAttribute="command" action="${submit_url}">
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
