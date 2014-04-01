<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>

<#function markingId user>
	<#if !user.warwickId?has_content || user.getExtraProperty("urn:websignon:usersource")! == 'WarwickExtUsers'>
		<#return user.userId />
	<#else>
		<#return user.warwickId />
	</#if>
</#function>

<div class="content online-feedback">
	<#if command.submission??>
		<#assign submission = command.submission />
		<#include "_submission_summary.ftl">
	</#if>
	<#if (isMarking!false) && (isRejected!false)>
		<#include "_rejection_summary.ftl">
	</#if>
	<#assign submit_url>
		<#if isMarking!false>
			<@routes.markerOnlinefeedbackform assignment markingId(command.student) />
		<#else>
			<@routes.onlinefeedbackform assignment markingId(command.student) />
		</#if>
	</#assign>
	<@f.form cssClass="form-horizontal double-submit-protection"
			method="post" enctype="multipart/form-data" commandName="command" action="${submit_url}">

		<#if (allCompletedMarkerFeedback?size > 1)>
			<@form.row cssClass="alert alert-success" >
				<@form.label><i class="icon-lightbulb"></i> Populate with previous</@form.label>
				<@form.field>
					<#list allCompletedMarkerFeedback as feedback>
						<a id="copy-complete-button" class="btn" title="Populate final feedback with ${feedback.getFeedbackPosition().description}">
							<i class="icon-arrow-down"></i>
							Copy ${feedback.getFeedbackPosition().description}
						</a>
					</#list>
				</@form.field>
			</@form.row>
		</#if>
		<#list assignment.feedbackFields as field>
			<div class="feedback-field">
				<#include "/WEB-INF/freemarker/submit/formfields/${field.template}.ftl">
			</div>
		</#list>

		<#if assignment.collectMarks>
			<@form.row>
				<@form.label path="mark">Mark</@form.label>
				<@form.field>
					<div class="input-append">
						<@f.input path="mark" cssClass="input-small" />
						<span class="add-on">%</span>
					</div>
					<@f.errors path="mark" cssClass="error" />
				</@form.field>
			</@form.row>
			<@form.row>
				<@form.label path="grade">Grade</@form.label>
				<@form.field>
					<@f.input path="grade" cssClass="input-small" />
					<@f.errors path="grade" cssClass="error" />
				</@form.field>
			</@form.row>
		</#if>

		<#if command.attachedFiles?has_content >
			<@form.labelled_row "attachedFiles" "Previously uploaded files">
				<ul class="unstyled">
					<#list command.attachedFiles as attachment>
						<li id="attachment-${attachment.id}">
							<span>${attachment.name}</span>&nbsp;
							<@f.hidden path="attachedFiles" value="${attachment.id}" />
							<a class="remove-attachment" href="">Remove</a>
						</li>
					</#list>
				</ul>
			</@form.labelled_row>
		</#if>

		<@form.labelled_row "file.upload" "Attachments">
			<input type="file" name="file.upload" />
		</@form.labelled_row>

		<div class="submit-buttons">
			<input class="btn btn-primary" type="submit" value="Save">
			<a class="btn discard-changes" href="">Discard</a>
		</div>

	</@f.form>
</div>