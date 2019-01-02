<#import "*/cm2_macros.ftl" as cm2 />
<#import "*/coursework_components.ftl" as components />
<#import "*/marking_macros.ftl" as marking />
<#escape x as x?html>
	<@cm2.assignmentHeader "Publish feedback" assignment "for" />
	<#assign module = assignment.module />
	<#assign department = module.adminDepartment />

	<#macro userList usercodes>
		<ul class="user-list">
			<@userlookup ids=usercodes>
				<#list returned_users?keys?sort as id>
					<#local returned_user=returned_users[id] />
					<#if returned_user.foundUser>
						<li>${returned_user.fullName} (${returned_user.warwickId!id})</li>
					<#else>
						<li>${id}</li>
					</#if>
				</#list>
			</@userlookup>
		</ul>
	</#macro>

	<#macro userListPopover collection entity="student" singular="does" plural="do" includeTrailingWord=true>
		<#assign popoverContent>
			<@userList collection/>
		</#assign>

		<a class="use-popover" href="#" data-html="true" data-content="${popoverContent}"><@fmt.p collection?size entity /></a>
		<#if includeTrailingWord>
			${(collection?size == 1)?string(singular, plural)}
		</#if>
	</#macro>

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

	<#assign submitUrl><@routes.cm2.publishFeedback assignment /></#assign>
	<@f.form method="post" action=submitUrl modelAttribute="command">
		<@bs3form.errors path="" />

		<#if submissionsReport.plagiarised?has_content>
			<div class="alert alert-danger">
				<h4>
					<i class="fa fa-fw fa-exclamation-circle"></i>
					Plagiarism
				</h4>

				<p>
					<@userListPopover submissionsReport.plagiarised "submission" "is" "are" /> flagged as plagiarised.
					To publish feedback to ${(submissionsReport.plagiarised?size == 1)?string("this student", "these students")}, you need to remove the plagiarism flag.
				</p>
			</div>
		</#if>

		<#if submissionsReport.publishable?size != 0>
			<div class="panel panel-default panel-body">
				<h4>
					<i class="fa fa-fw fa-${recipientCheckReport.hasProblems?string('info-circle', 'check-circle')}"></i>
					Feedback emails
				</h4>

				<#if features.emailStudents>
					<p>
						When you publish feedback, each student receives an email with a link to view their feedback.
					</p>
				<#else>
					<p>
						Email notifications are disabled. Students can view their feedback by signing into Tabula and selecting
						Coursework Management.
					</p>
				</#if>

				<#if recipientCheckReport.hasProblems>
					<#assign popoverContent>
						<ul>
						<#list recipientCheckReport.problems as problem>
							<li>
								${problem.universityId}:
								<#if problem.user??>
									email address '${problem.user.email!}' is invalid
								<#else>
									user not found
								</#if>
							</li>
						</#list>
						</ul>
					</#assign>

					<p>
						Email addresses for
						<a class="use-popover" href="#" data-html="true" data-content="${popoverContent}"><@fmt.p recipientCheckReport.problems?size "student" /></a>
						not found.

						<#if recipientCheckReport.problems?size == 1>
							This student
						<#else>
							These students
						</#if>
						can view their feedback by signing into Tabula and selecting Coursework Management.
					</p>
				</#if>
			</div>
		</#if>

		<div class="panel panel-default panel-body">
			<h4>
				<i class="fa fa-fw fa-${(submissionsReport.hasProblems || submissionsReport.publishable?size == 0)?string('info-circle', 'check-circle')}"></i>
				Submissions and feedback
			</h4>

			<#if submissionsReport.publishable?size == 0>
			<p>
				No feedback is available to publish.
			</p>
			<#else>
			<p>
				You can publish feedback to <@fmt.p submissionsReport.publishable?size "student" />.
				<#if submissionsReport.hasProblems>
					Of these:
				</#if>
			</p>
			</#if>

			<#if submissionsReport.hasProblems>
				<ul>
					<#if submissionsReport.feedbackOnly?has_content>
						<li><@userListPopover submissionsReport.feedbackOnly "student" "has" "have" /> marks or feedback, but did not submit coursework to Tabula</li>
					</#if>
					<#if submissionsReport.withoutMarks?has_content>
						<li><@userListPopover submissionsReport.withoutMarks /> not have a mark</li>
					</#if>
					<#if submissionsReport.submissionOnly?has_content>
						<li><@userListPopover submissionsReport.submissionOnly /> not have feedback</li>
					</#if>
					<#if submissionsReport.withoutAttachments?has_content>
						<li><@userListPopover submissionsReport.withoutAttachments /> not have any feedback files attached</li>
					</#if>
					<#if submissionsReport.alreadyPublished?has_content>
						<li><@userListPopover submissionsReport.alreadyPublished "student" "has" "have" /> already received feedback, so won't be sent another email</li>
					</#if>
				</ul>
			<#elseif submissionsReport.publishable?size != 0>
				<p>
					No warnings found when checking submissions and feedback for this assignment.
				</p>
			</#if>
		</div>

		<#list command.feedbackToRelease as feedback>
			<@f.hidden name="students" value=feedback.usercode />
		</#list>

		<#if command.feedbackToRelease?size != 0>
			<@bs3form.form_group path="confirm">
				<@bs3form.checkbox path="confirm">
					<@f.checkbox path="confirm" id="confirmCheck" /> I've reviewed these messages and am ready to publish feedback to
				<strong><@fmt.p submissionsReport.publishable?size "student" /></strong>
				</@bs3form.checkbox>
			</@bs3form.form_group>

			<#if features.queueFeedbackForSits && department.uploadCourseworkMarksToSits>
				<@marking.uploadToSits assignment=assignment verb="published" withValidation=true isGradeValidation=isGradeValidation gradeValidation=gradeValidation />
			</#if>

			<div class="submit-buttons">
				<button id="publish-submit" type="submit" class="btn btn-primary">Publish feedback</button>
				<a class="btn btn-default" href="<@routes.cm2.assignmentsubmissionsandfeedback assignment />">Cancel</a>
			</div>
		</#if>
	</@f.form>
</#escape>