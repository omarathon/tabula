<#escape x as x?html>

<h1>Reject and return to ${previousStageRole}</h1>

<@f.form cssClass="submission-form double-submit-protection form-horizontal" method="post" action="${formAction}" commandName="markingUncompletedCommand">

	<@form.errors path="" />

	<input type="hidden" name="confirmScreen" value="true" />

	<#if markingUncompletedCommand.students?has_content>
		<#list markingUncompletedCommand.students as student>
			<input type="hidden" name="students" value="${student}" />
		</#list>
	</#if>

	<@spring.bind path="markerFeedback">
		<@form.errors path="markerFeedback" />
		<#assign markerFeedback = status.actualValue />
		<p>
			<strong>Feedback for <@fmt.p markingUncompletedCommand.markerFeedback?size "submission" /></strong> will be rejected and returned to the ${previousStageRole}.
		</p>
		<#list markerFeedback as mf>
			<input type="hidden" name="markerFeedback" value="${mf.id}" />
		</#list>
	</@spring.bind>


	<@form.row path="comment">
		<@form.label for="assignmentComment">Comments to send to the previous marker</@form.label>
		<@form.field>
			<@f.errors path="comment" cssClass="error" />
			<@f.textarea path="comment" rows="6" cssClass="span6" />
		</@form.field>
	</@form.row>

	<@form.row>
		<@form.label></@form.label>
		<@form.field>
		<label class="checkbox">
			<@f.errors path="confirm" cssClass="error" />
			<@f.checkbox path="confirm" />
			I confirm that I would like to return <@fmt.p markerFeedback?size "this student's submission" "these students' submissions" "1" "0" false /> to the ${previousStageRole}.
		</label>
		</@form.field>
	</@form.row>

	<div class="submit-buttons">
		<input class="btn btn-primary" type="submit" value="Confirm">
		<a class="btn" href="<@routes.listmarkersubmissions assignment marker />">Cancel</a>
	</div>
</@f.form>
</#escape>