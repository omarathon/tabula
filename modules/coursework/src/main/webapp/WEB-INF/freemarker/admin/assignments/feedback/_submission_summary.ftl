<#import "../submissionsandfeedback/_submission_details.ftl" as sd />
<#import "*/submission_components.ftl" as components />

<div class="well">
	<h3>Submission</h3>

	<div class="labels">
		<#if submission.late>
			<span class="label label-important use-tooltip" title="<@sd.lateness submission />" data-container="body">Late</span>
		<#elseif  submission.authorisedLate>
			<span class="label label-info use-tooltip" title="<@sd.lateness submission />" data-container="body">Within Extension</span>
		</#if>

		<#if submission.suspectPlagiarised>
			<span class="label label-important use-tooltip" title="Suspected of being plagiarised" data-container="body">Plagiarism suspected</span>
		<#elseif submission.investigationCompleted>
			<span class="label label-info use-tooltip" title="No evidence of plagiarism was found" data-container="body">Plagiarism investigation completed</span>
		</#if>
	</div>

	<div>
		<@spring.message code=command.submissionState /><@sd.submission_details command.submission />

		<#list submission.allAttachments as attachment>
			<!-- Checking originality report for ${attachment.name} ... -->
			<#if attachment.originalityReport??>
				<@components.originalityReport attachment />
			</#if>
		</#list>

		<br />
		Due date: <@fmt.date date=submission.deadline capitalise=true shortMonth=true />
	</div>
</div>
