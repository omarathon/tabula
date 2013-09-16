<#import "../submissionsandfeedback/_submission_details.ftl" as sd />
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
