<#import "*/cm2_macros.ftl" as cm2 />

<#escape x as x?html>
	<#if nextStagesDescription??>
		<@cm2.assignmentHeader "Send to ${nextStagesDescription?lower_case}" assignment "for" />
	</#if>


	<#assign formAction><@routes.cm2.markingCompleted assignment stagePosition marker /></#assign>
	<@f.form method="post" action="${formAction}" commandName="command">
		<@form.errors path="" />
		<input type="hidden" name="confirmScreen" value="true" />

		<@spring.bind path="markerFeedback">
			<@bs3form.errors path="markerFeedback" />
			<#assign markerFeedback=status.actualValue />
			<#assign noMarks = command.noMarks />
			<#assign noFeedback = command.noFeedback />
			<#assign releasedFeedback = command.releasedFeedback />

			<#if releasedFeedback?has_content>
				<div class="alert alert-info">
						<#assign releasedFeedbackIds><ul><#list releasedFeedback as markerFeedback><li>${markerFeedback.feedback.studentIdentifier}</li></#list></ul></#assign>
						<a class="use-popover"
							 data-html="true"
							 data-original-title="<span class='text-info'><strong>Already released</strong></span>"
							 data-content="${releasedFeedbackIds}">
							<@fmt.p (releasedFeedback?size ) "submission" />
						</a>
					<#if releasedFeedback?size == 1>
						has already been marked as completed. This will be ignored.
					<#else>
						have already been marked as completed. These will be ignored.
					</#if>
				</div>
			</#if>

			<#if (assignment.collectMarks && noMarks?size > 0)>
				<#assign count><#if (noMarks?size > 1)>	${noMarks?size} submissions do<#else>One submission does</#if></#assign>
				<#assign noMarksIds><ul><#list noMarks as markerFeedback><li>${markerFeedback.feedback.studentIdentifier}</li></#list></ul></#assign>
				<div class="alert alert-info">
					${count} not have a mark. You will not be able to add a mark to these submissions later.
					<a class="use-popover" id="popover-marks" data-html="true"
						 data-original-title="<span class='text-info'><strong>No marks</strong></span>"
						 data-content="${noMarksIds}">
						<i class="fa fa-question-sign"></i>
					</a>
				</div>
			</#if>

			<#if (noFeedback?size > 0) >
				<#assign count><#if (noFeedback?size > 1)>${noFeedback?size} submissions do<#else>One submission does</#if></#assign>
				<#assign noFilesIds>
				<ul><#list noFeedback as markerFeedback><li>${markerFeedback.feedback.studentIdentifier}</li></#list></ul>
				</#assign>
				<div class="alert alert-info">
					${count} not have any feedback files attached. 	You will not be able to add feedback comments or files to this submission later.
					<a class="use-popover" id="popover-files" data-html="true"
						 data-original-title="<span class='text-info'><strong>No feedback files</strong></span>"
						 data-content="${noFilesIds}">
						<i class="fa fa-question-sign"></i>
					</a>
				</div>
			</#if>

			<p>
				<strong><@fmt.p (command.feedbackForRelease?size) "student" /></strong> submissions will be listed as completed.
				Note that you will not be able to make any further changes to the marks or feedback associated with these submissions after this point.
				If there are still changes that have to be made for these submissions then click cancel to return to the feedback list.
			</p>

			<#list markerFeedback as mf>
				<input type="hidden" name="markerFeedback" value="${mf.id}" />
			</#list>
		</@spring.bind>

		<@bs3form.form_group>
			<@bs3form.checkbox path="confirm">
				<@f.checkbox path="confirm" /> I confirm that I have finished marking <@fmt.p markerFeedback?size "this" "these" "1" "0" false /> student's submissions.
			</@bs3form.checkbox>
		</@bs3form.form_group>

		<div class="buttons">
			<input class="btn btn-primary" type="submit" value="Confirm">
			<a class="btn btn-default" href="<@routes.cm2.listmarkersubmissions assignment marker />">Cancel</a>
		</div>
	</@f.form>
</#escape>