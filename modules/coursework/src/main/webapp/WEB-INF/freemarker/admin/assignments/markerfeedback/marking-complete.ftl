<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>
<#assign formAction><@url page='/admin/module/${module.code}/assignments/${assignment.id}/marker/marking-completed'/></#assign>

<@f.form method="post" action="${formAction}" commandName="markingCompletedCommand">

<h1>Marking completed</h1>

<@form.errors path="" />

<input type="hidden" name="onlineMarking" value="${onlineMarking?string}" />
<input type="hidden" name="confirmScreen" value="true" />

<@spring.bind path="students">
	<@form.errors path="students" />
	<#assign students=status.actualValue />
	<#assign noMarks = markingCompletedCommand.noMarks />
	<#assign noFeedback = markingCompletedCommand.noFeedback />
	<#assign releasedFeedback = markingCompletedCommand.releasedFeedback />

	<#if releasedFeedback?has_content>
		<div class="alert">
			<a href="" class="released-feedback"><@fmt.p (releasedFeedback?size ) "submission" /></a>
			<#if markingCompletedCommand.releasedFeedback?size == 1>
				has already been marked as completed. This will be ignored.
			<#else>
				have already been marked as completed. These will be ignored.
			</#if>
			<div class="hidden released-feedback-list">
				<ul><#list releasedFeedback as markerFeedback>
					<li>${markerFeedback.feedback.universityId}</li>
				</#list></ul>
			</div>
			<script>
				var listHtml = jQuery(".released-feedback-list").html();
				jQuery(".released-feedback").on('click',function(e){e.preventDefault()})
				jQuery(".released-feedback").tooltip({
					html: true,
					placement: 'right',
					title: listHtml
				});
			</script>
		</div>
	</#if>
	<#if (noMarks?size > 0) >
		<div class="alert">
			<#if (noMarks?size > 1)>
				${noFeedback?size} submissions do not have a mark. You will not be able to add a mark to these submissions later.
			<#else>
				One submission does not have a mark. You will not be able to add a mark to this submission later.
			</#if>
		</div>
	</#if>
	<#if (noFeedback?size > 0) >
		<div class="alert">
			<#if (noFeedback?size > 1)>
				${noFeedback?size} submissions do not have any feedback files attached.
				You will not be able to add feedback files to these submissions later.
			<#else>
				One submission does not have any feedback files attached.
				You will not be able to add feedback files to this submission later.
			</#if>
		</div>
	</#if>
	<p>
		<strong><@fmt.p (students?size - releasedFeedback?size) "student" /></strong> submissions will be listed as completed. Note that you will not be able
		to make any further changes to the marks or feedback associated with these submissions after this point. If there are
		still changes that have to be made for these submission then click cancel to return to the feedback list.
	</p>
	<#list students as uniId>
		<input type="hidden" name="students" value="${uniId}" />
	</#list>
</@spring.bind>
<p>
	<@form.errors path="confirm" />
	<@form.label checkbox=true><@f.checkbox path="confirm" />
		<#if (students?size > 1)>
			I confirm that I have finished marking these student's submissions.
		<#else>
			I confirm that I have finished marking this student's submission.
		</#if>
	</@form.label>
</p>

<div class="submit-buttons">
	<input class="btn btn-warn" type="submit" value="Confirm">
	<a class="btn" href="<@routes.listmarkersubmissions assignment />">Cancel</a>
</div>
</@f.form>
</#escape>