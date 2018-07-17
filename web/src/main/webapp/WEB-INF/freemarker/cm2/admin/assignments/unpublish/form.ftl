<#import "*/cm2_macros.ftl" as cm2 />
<#import "*/coursework_components.ftl" as components />
<#import "*/marking_macros.ftl" as marking />
<#escape x as x?html>
	<@cm2.assignmentHeader "Unpublish feedback" assignment "for" />
	<#assign module = assignment.module />
	<#assign department = module.adminDepartment />

	<script type="text/javascript">
		jQuery(function ($) {
			"use strict";
			var submitButton = $('#publish-submit'),
				checkbox = $('#confirmCheck');

			function updateCheckbox() {
				submitButton.attr('disabled', !checkbox.is(':checked'));
			}

			checkbox.change(updateCheckbox);
			updateCheckbox();
		});
	</script>

	<#assign submitUrl><@routes.cm2.unPublishFeedback assignment /></#assign>
	<@f.form method="post" action=submitUrl commandName="command">
		<@bs3form.errors path="" />

		<#list command.feedbackToUnPublish as feedback>
			<@f.hidden name="students" value=feedback.usercode />
		</#list>

		<#if command.feedbackToUnPublish?size != 0>
			<@bs3form.form_group path="confirm">
				<@bs3form.checkbox path="confirm">
					<@f.checkbox path="confirm" id="confirmCheck" /> I am ready to unpublish feedback for
					<strong><@fmt.p command.feedbackToUnPublish?size "student" /></strong>
				</@bs3form.checkbox>
			</@bs3form.form_group>

			<div class="submit-buttons">
				<button id="publish-submit" type="submit" class="btn btn-primary">Unpublish feedback</button>
				<a class="btn btn-default" href="<@routes.cm2.assignmentsubmissionsandfeedback assignment />">Cancel</a>
			</div>
		</#if>
	</@f.form>
</#escape>