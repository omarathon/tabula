<#macro lateness submission="" assignment="" user=""><#compress>
	<#if submission?has_content && submission.submittedDate?? && (submission.late || submission.authorisedLate)>
		<#if submission.late>
			<@fmt.p submission.workingDaysLate "working day" /> late, ${durationFormatter(submission.deadline, submission.submittedDate)} after deadline
		<#else>
			${durationFormatter(submission.assignment.closeDate, submission.submittedDate)} after close
		</#if>
	<#elseif assignment?has_content && user?has_content>
		<#local lateness = assignment.workingDaysLateIfSubmittedNow(user.userId) />
		<@fmt.p lateness "working day" /> overdue, the deadline/extension was ${durationFormatter(assignment.submissionDeadline(user.userId))}
	</#if>
</#compress></#macro>

<#macro extensionLateness extension submission><#compress>
	<#if extension?has_content && extension.expiryDate?? && submission.late>
		<@fmt.p submission.workingDaysLate "working day" /> late, ${durationFormatter(extension.expiryDate, submission.submittedDate)} after extended deadline (<@fmt.date date=extension.expiryDate capitalise=false shortMonth=true stripHtml=true />)
	</#if>
</#compress></#macro>

<#macro submission_details submission=[]><@compress single_line=true>
	<#if submission?has_content>
		<#local attachments = submission.allAttachments />
		<#local assignment = submission.assignment />
		<#local module = assignment.module />

		<#if submission.submittedDate??>
			<span class="date use-tooltip" title="<@lateness submission />" data-container="body">
				<@fmt.date date=submission.submittedDate seconds=true capitalise=true shortMonth=true />
			</span>
		</#if>

		<#if attachments?size gt 0>
			<#if attachments?size == 1>
				<#local filename = "${attachments[0].name}">
				<#local downloadUrl><@routes.cm2.downloadSubmission submission filename/>?single=true</#local>
			<#else>
				<#local filename = "submission-${submission.studentIdentifier}.zip">
				<#local downloadUrl><@routes.cm2.downloadSubmission submission filename/></#local>
			</#if>
			&emsp;<a class="long-running" href="${downloadUrl}">Download submission</a>
		</#if>
	</#if>
</@compress></#macro>

<#macro submission_status submission="" enhancedExtension="" enhancedFeedback="" student="">
	<#if submission?has_content>
		<#if submission.late>
			<#if enhancedExtension?has_content && enhancedExtension.extension.approved>
				<span class="label label-important use-tooltip" title="<@extensionLateness enhancedExtension.extension submission/>" data-container="body">Late</span>
			<#else>
				<span class="label label-important use-tooltip" title="<@lateness submission />" data-container="body">Late</span>
			</#if>
		<#elseif submission.authorisedLate>
			<span class="label label-info use-tooltip" data-html="true" title="Extended until <@fmt.date date=enhancedExtension.extension.expiryDate capitalise=false shortMonth=true />" data-container="body">Within Extension</span>
		</#if>
		<#if features.disabilityOnSubmission && student.disability??>
			<a class="use-popover cue-popover" id="popover-disability" data-html="true"
			   data-original-title="Disability disclosed"
			   data-content="<p>This student has chosen to make the marker of this submission aware of their disability and for it to be taken it into consideration. This student has self-reported the following disability code:</p><div class='well'><h6>${student.disability.code}</h6><small>${(student.disability.sitsDefinition)!}</small></div>"
			>
				<span class="label label-info">Disability disclosed</span>
			</a>
		</#if>
	<#elseif !enhancedFeedback?has_content>
		<span class="label label-info">Unsubmitted</span>
		<#if enhancedExtension?has_content>
			<#local extension=enhancedExtension.extension>
			<#if extension.approved && !extension.rejected>
				<#local date>
					<@fmt.date date=extension.expiryDate capitalise=true shortMonth=true stripHtml=true />
				</#local>
			</#if>
			<#if enhancedExtension.within>
				<span class="label label-info use-tooltip" data-html="true" title="${date}" data-container="body">Within Extension</span>
			<#elseif extension.rejected>
				<span class="label label-info">Extension Rejected</span>
			<#elseif !extension.approved>
				<span class="label label-info">Extension Requested</span>
			<#else>
				<span class="label label-info use-tooltip" title="${date}" data-container="body">Extension Expired</span>
			</#if>
		</#if>
	</#if>
</#macro>

<#macro originalityReport attachment>
	<#local r=attachment.originalityReport />
	<#local assignment=attachment.submissionValue.submission.assignment />

	<span id="tool-tip-${attachment.id}" class="similarity-${r.similarity} similarity-tooltip">${r.overlap}% similarity</span>
	<div id="tip-content-${attachment.id}" class="hide">
		<p>${attachment.name} <img src="<@url resource="/static/images/icons/turnitin-16.png"/>"></p>
		<p class="similarity-subcategories-tooltip">
			Web: ${r.webOverlap}%<br>
			Student papers: ${r.studentOverlap}%<br>
			Publications: ${r.publicationOverlap}%
		</p>
		<p>
			<a target="turnitin-viewer" href="<@routes.cm2.turnitinLtiReport assignment attachment />">View full report</a>
		</p>
	</div>
	<script type="text/javascript">
		jQuery(function($){
			$("#tool-tip-${attachment.id}").popover({
				placement: 'right',
				html: true,
				content: function(){return $('#tip-content-${attachment.id}').html();},
				title: 'Turnitin report summary'
			});
		});
	</script>
</#macro>
