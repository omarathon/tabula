<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#import "../submissionsandfeedback/_submission_details.ftl" as sd />

<div class="feedback">
	<#if command.submission??>
		<#assign submission = command.submission />

		<div class="well">
			<h3>Submission</h3>

			<div class="labels">
				<#if submission.late>
					<span class="label label-important use-tooltip" title="<@sd.lateness submission />" data-container="body">Late</span>
				<#elseif  submission.authorisedLate>
					<span class="label label-info use-tooltip" title="<@sd.lateness submission />" data-container="body">Within Extension</span>
				</#if>

				<#if submission.suspectPlagiarised>
					<span class="label label-important use-tooltip" title="Suspected of being plagiarised" data-container="body">Plagiarism Suspected</span>
				</#if>
			</div>

			<div>
				<@spring.message code=command.submissionState /><@sd.submission_details command.submission />
			</div>
		</div>
	</#if>
	<div class="form">
		<@f.form class="form-horizontal" method="post" enctype="multipart/form-data" commandName="command">

			<@form.row>
				<@form.label path="mark">Mark</@form.label>
				<@form.field>
					<@f.input path="mark" cssClass="input-small" />
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

		</@f.form>
	</div>
</div>