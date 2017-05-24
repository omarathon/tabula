<#import "*/_submission_details.ftl" as sd />

<div role="tabpanel" class="tab-pane active" id="${student.userId}${command.stage.name}submission">
	<#if command.submission?has_content>
		<#assign submission = command.submission />
		<ul class="list-unstyled">

			<li><strong><@spring.message code=command.submissionState />:</strong><@sd.submission_details submission /></li>

			<#if assignment.wordCountField?? && submission.valuesByFieldName[assignment.defaultWordCountName]??>
				<li><strong>Word count:</strong> ${submission.valuesByFieldName[assignment.defaultWordCountName]?number}</li>
			</#if>

			<li>
				<strong>Plagiarism check:</strong>
				<#if submission.hasOriginalityReport>
					<@compress single_line=true>
						<@fmt.p submission.attachmentsWithOriginalityReport?size "file" /><#if submission.suspectPlagiarised>, marked as plagiarised<#elseif submission.investigationCompleted>, plagiarism investigation completed</#if>
					</@compress>
					<div class="originality-reports">
						<#list submission.attachmentsWithOriginalityReport as attachment>
							<@sd.originalityReport attachment />
						</#list>
					</div>
				<#else>
					This submission has not been checked for plagiarism
				</#if>
			</li>

			<#if features.disabilityOnSubmission && command.disability??>
				<li>
					<strong>Disability disclosed:</strong>
					<a class="use-popover cue-popover" id="popover-disability" data-html="true"
						 data-original-title="Disability disclosed"
						 data-container="body"
						 data-content="<p>This student has chosen to make the marker of this submission aware of their disability and for it to be taken it into consideration. This student has self-reported the following disability code:</p><div class='well'><h6>${command.disability.code}</h6><small>${(command.disability.sitsDefinition)!}</small></div>"
					>
						See details
					</a>
				</li>
			</#if>
		</ul>
	<#else>
		<#assign wasIs><#if command.assignment.isClosed() && !command.assignment.isWithinExtension(command.student)>was<#else>is</#if></#assign>
		<#assign dueDate = command.assignment.submissionDeadline(command.student) />
		<span>This student has not submitted yet. Their submission ${wasIs} due on <@fmt.date date=dueDate capitalise=true shortMonth=true /></span>
	</#if>
	<#if command.extension?has_content>
	<ul class="list-unstyled">
		<li>
			<strong>Extension:</strong> <@spring.message code=command.extensionState /> for <@fmt.date date=command.extensionDate capitalise=true shortMonth=true />
		</li>
	</ul>
	</#if>
</div>