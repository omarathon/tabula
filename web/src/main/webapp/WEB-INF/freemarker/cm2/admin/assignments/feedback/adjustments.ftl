<#import "*/cm2_macros.ftl" as cm2 />
<#import "*/coursework_components.ftl" as components />
<#import "*/marking_macros.ftl" as marking />

<#assign finalMarkingStage = (allCompletedMarkerFeedback?? && allCompletedMarkerFeedback?size > 1)>

<div class="content feedback-adjustment feedback-summary">

	<#if command.submission??>
		<#assign submission = command.submission />
		<#include "_submission_summary.ftl">
	</#if>

	<#if command.feedback?? && (command.feedback.actualGrade?has_content || command.feedback.actualMark?has_content)>
		<div class="well">
			<h3>Feedback</h3>
			<p>
				<#if command.feedback.actualMark??>
					Original mark - ${command.feedback.actualMark}%<br>
				</#if>
				<#if command.feedback.actualGrade??>
					Original grade - ${command.feedback.actualGrade}<br>
				</#if>
			</p>
		</div>

		<#if command.feedback.latestPrivateOrNonPrivateAdjustment?has_content>
			<div class="well">
				<h3>Latest adjustment</h3>
				<p>
					Adjusted mark - ${command.feedback.latestPrivateOrNonPrivateAdjustment.mark!}<br>
					Adjusted grade - ${command.feedback.latestPrivateOrNonPrivateAdjustment.grade!}<br>
					<#if command.feedback.latestPrivateOrNonPrivateAdjustment.markType.code == "private">
						This is a private adjustment that is not visible to the student
					</#if>
				</p>
			</div>
		</#if>
	</#if>

	<#assign submit_url>
		<@routes.cm2.feedbackAdjustmentForm assignment marking.extractId(command.student) />
	</#assign>

	<@f.form cssClass="dirty-check double-submit-protection ajax-form"
			 method="post"
			 commandName="command"
			 action="${submit_url}">

		<@bs3form.errors path="" />

		<@bs3form.labelled_form_group path="reason" labelText="Reason for adjustment">
			<@f.select path="reason" class="form-control">
				<@f.option></@f.option>
				<@f.option value="Late submission penalty">Late submission penalty</@f.option>
				<@f.option value="Plagarism penalty">Plagarism penalty</@f.option>
				<@f.option value="Other">Other</@f.option>
			</@f.select>
			<@f.input type="text" path="reason" cssClass="form-control hide other-input" placeholder="Enter your reason" disabled=true/>
		</@bs3form.labelled_form_group>

		<@bs3form.labelled_form_group  path="comments" labelText="Adjustment comments">
			<@f.textarea path="comments" cssClass="form-control text big-textarea" maxlength=4000 />
		</@bs3form.labelled_form_group>

		<@bs3form.labelled_form_group path="adjustedMark" labelText="Adjusted mark">
			<div class="input-group">
				<@f.input type="number" path="adjustedMark" cssClass="form-control" />
				<span class="input-group-addon">%</span>
			</div>
			<#if proposedAdjustment??>
				<div class="late-penalty">
					<button class="btn btn-xs use-suggested-mark"
						data-mark="${proposedAdjustment!""}"
						data-comment="Your submission was <@components.lateness command.submission /> late. ${marksSubtracted} marks have been subtracted (${latePenalty} for each working day late).">
							Use suggested mark - ${proposedAdjustment!""}
					</button>
					<a class="use-popover cue-popover" id="popover-${marking.extractId(command.student)}" data-html="true"
						 data-original-title="Late penalty calculation"
						 data-container="body"
						 data-content="The submission was <@fmt.p daysLate "working day" /> late. The suggested penalty
							 was derived by subtracting ${latePenalty} marks from the actual mark for each day the submission
							 was late.">
						<i class="fa fa-question-circle"></i>
					</a>
				</div>
			</#if>
		</@bs3form.labelled_form_group>

		<@bs3form.labelled_form_group path="adjustedGrade" labelText="">
			<div class="input-group">
				<#if isGradeValidation>
					<#assign generateUrl><@routes.cm2.generateGradesForMarks command.assignment /></#assign>
					<@marking.autoGradeOnline "adjustedGrade" "Adjusted grade" "adjustedMark" marking.extractId(command.student) generateUrl />
				<#else>
					<@f.input path="adjustedGrade" cssClass="form-control" />
				</#if>
			</div>
		</@bs3form.labelled_form_group>

		<div class="alert alert-info">
			The reason for adjustment and any comments will be made available to students when their feedback is published.
		</div>

		<#if features.queueFeedbackForSits && assignment.module.adminDepartment.uploadCourseworkMarksToSits && command.canBeUploadedToSits>
			<@marking.uploadToSits assignment=assignment verb="Adjusting" withValidation=gradeValidation?? isGradeValidation=isGradeValidation gradeValidation=gradeValidation />
		</#if>

		<div class="buttons form-group">
			<button type="submit" class="btn btn-primary">Save</button>
			<a class="btn btn-default cancel" href="#">Cancel</a>
		</div>
	</@f.form>
</div>