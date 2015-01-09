<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>
<#assign formAction>
	<#if nextStageRole?has_content><@routes.markingCompleted assignment marker nextStageRole/>
	<#else><@routes.markingCompleted assignment marker />
	</#if>
</#assign>

<@f.form method="post" action="${formAction}" commandName="markingCompletedCommand">

<#assign title><#if nextStageRole?has_content>Send to ${nextStageRole}<#else>Marking completed</#if></#assign>

<h1>Send to ${nextStageRole}</h1>

<@form.errors path="" />

<input type="hidden" name="onlineMarking" value="${onlineMarking?string}" />
<input type="hidden" name="confirmScreen" value="true" />

<@spring.bind path="markerFeedback">
	<@form.errors path="markerFeedback" />
	<#assign markerFeedback=status.actualValue />
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
				jQuery(".released-feedback")
					.on('click',function(e){e.preventDefault()})
					.tooltip({
						html: true,
						placement: 'right',
						title: listHtml
					});
			</script>
		</div>
	</#if>

	<#if (assignment.collectMarks && noMarks?size > 0) >
		<#if isUserALaterMarker>
			<div class="alert alert-info">
				<#if (noMarks?size > 1)>
					${noFeedback?size} submissions do not have a mark. You will not be able to change this before these submissions are marked by others.
				<#else>
					One submission does not have a mark. You will not be able to change this before this submission is marked by others.
				</#if>
			</div>
		<#else>
			<div class="alert">
				<#if (noMarks?size > 1)>
					${noFeedback?size} submissions do not have a mark. You will not be able to add a mark to these submissions later.
				<#else>
					One submission does not have a mark. You will not be able to add a mark to this submission later.
				</#if>
			</div>
		</#if>
	</#if>

	<#if (noFeedback?size > 0) >
		<#if isUserALaterMarker>
			<div class="alert alert-info">
				<#if (noMarks?size > 1)>
					${noFeedback?size} submissions do not have any feedback files attached.
					You will not be able to change this before these submissions are marked by others.
				<#else>
					One submission does not have any feedback files attached.
					You will not be able to change this before this submission is marked by others.
				</#if>
			</div>
		<#else>
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
	</#if>

	<#if isUserALaterMarker>
		<p>
			<strong><@fmt.p (markerFeedback?size - releasedFeedback?size) "student" /></strong> submissions will be listed as completed.
			You will not be able to make further changes to the marks or feedback associated with these submissions until they have been marked by others.
			If there are still changes that have to be made for these submissions then click cancel to return to the feedback list.
		</p>
	<#else>
		<p>
			<strong><@fmt.p (markerFeedback?size - releasedFeedback?size) "student" /></strong> submissions will be listed as completed.
			Note that you will not be able to make any further changes to the marks or feedback associated with these submissions after this point.
			If there are still changes that have to be made for these submissions then click cancel to return to the feedback list.
		</p>
	</#if>

	<#list markerFeedback as mf>
		<input type="hidden" name="markerFeedback" value="${mf.id}" />
	</#list>
</@spring.bind>
<p>
	<@form.errors path="confirm" />
	<@form.label checkbox=true><@f.checkbox path="confirm" />
		I confirm that I have finished marking <@fmt.p markerFeedback?size "this" "these" "1" "0" false /> student's submissions.
	</@form.label>
</p>

<div class="submit-buttons">
	<input class="btn btn-warn" type="submit" value="Confirm">
	<a class="btn" href="<@routes.listmarkersubmissions assignment marker />">Cancel</a>
</div>
</@f.form>
</#escape>