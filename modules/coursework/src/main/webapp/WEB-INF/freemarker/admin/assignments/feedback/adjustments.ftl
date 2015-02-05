<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#assign finalMarkingStage = (allCompletedMarkerFeedback?? && allCompletedMarkerFeedback?size > 1)>

<#macro lateness submission=""><#compress>
	${durationFormatter(submission.deadline, submission.submittedDate)}
</#compress></#macro>

<#function markingId user>
	<#if !user.warwickId?has_content || user.getExtraProperty("urn:websignon:usersource")! == 'WarwickExtUsers'>
		<#return user.userId />
	<#else>
		<#return user.warwickId! />
	</#if>
</#function>

<div class="content feedback-adjustment feedback-summary">

<#if command.submission??>
	<#assign submission = command.submission />
	<#include "_submission_summary.ftl">

	<#if command.actualGrade?? || command.actualMark??>
		<div class="well">
			<h3>Feedback</h3>
			<p>
				<#if command.actualMark??>
					Actual mark - ${command.actualMark}<br>
				</#if>
				<#if command.actualGrade??>
					Actual grade - ${command.actualGrade}<br>
				</#if>
			</p>
		</div>
	</#if>

</#if>

<#assign submit_url>
	<@routes.feedbackAdjustmentForm assignment markingId(command.student) />
</#assign>

<@f.form cssClass="form-horizontal double-submit-protection"
		 method="post"
		 commandName="command"
		 action="${submit_url}">

	<@form.row>
		<@form.label path="reason">Reason for adjustment</@form.label>
		<@form.field>
			<@f.select path="reason">
				<@f.option></@f.option>
				<@f.option value="Late submission penalty">Late submission penalty</@f.option>
				<@f.option value="Plagarism penalty">Plagarism penalty</@f.option>
				<@f.option value="Other">Other</@f.option>
			</@f.select>
			<@f.input type="text" path="reason" cssClass="hide other-input" placeholder="Enter your reason" disabled=true/>
			<@f.errors path="reason" cssClass="error" />
		</@form.field>
	</@form.row>

	<@form.row>
		<@form.label path="comments">Adjustment comments</@form.label>
		<@form.field>
			<@f.textarea path="comments" cssClass="big-textarea" />
			<@f.errors path="comments" cssClass="error" />
		</@form.field>
	</@form.row>

	<@form.row>
		<@form.label path="adjustedMark">Adjusted mark</@form.label>
		<@form.field>
			<div class="input-append">
				<@f.input path="adjustedMark" cssClass="input-small" />
				<span class="add-on">%</span>
			</div>
			<#if proposedAdjustment??>
				<div class="late-penalty">
					<button class="btn btn-mini use-suggested-mark"
							data-mark="${proposedAdjustment!""}"
							data-comment="Your submission was <@lateness command.submission /> late. ${marksSubtracted} marks have been subtracted (${latePenalty} for each working day late).">
						Use suggested mark - ${proposedAdjustment!""}
					</button>
					<a class="use-popover" id="popover-${markingId(command.student)}" data-html="true"
					   data-original-title="<span class='text-info'><strong>Late penalty calculation</strong></span>"
					   data-content="The submission was <@fmt.p daysLate "working day" /> late. The suggested penalty
					   was derived by subtracting ${latePenalty} marks from the actual mark for each day the submission
					   was late.">
						<i class="icon-question-sign"></i>
					</a>
				</div>
			</#if>
			<@f.errors path="adjustedMark" cssClass="error" />
		</@form.field>
	</@form.row>

	<@form.row>
		<@form.label path="adjustedGrade">Adjusted grade</@form.label>
		<@form.field>
			<@f.input path="adjustedGrade" cssClass="input-small" />
			<@f.errors path="adjustedGrade" cssClass="error" />
		</@form.field>
	</@form.row>

	<div class="alert alert-info">
		The reason for adjustment and any comments will be made available to students when their feedback is published.
	</div>

	<div class="submit-buttons">
		<input class="btn btn-primary" type="submit" value="Save">
		<a class="btn discard-changes" href="">Cancel</a>
	</div>

</@f.form>
</div>

