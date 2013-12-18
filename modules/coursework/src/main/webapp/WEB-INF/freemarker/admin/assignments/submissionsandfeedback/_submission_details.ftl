<#macro lateness submission=""><#compress>
	<#if submission?has_content && submission.submittedDate?? && (submission.late || submission.authorisedLate)>
		${durationFormatter(assignment.closeDate, submission.submittedDate)} after close
	</#if>
</#compress></#macro>

<#macro extensionLateness extension submission><#compress>
	<#if extension?has_content && extension.expiryDate?? && submission.late>
	${durationFormatter(extension.expiryDate, submission.submittedDate)} after extended deadline (<@fmt.date date=extension.expiryDate capitalise=false shortMonth=true />)
	</#if>
</#compress></#macro>

<#macro submission_details submission=""><#compress>
	<#if submission?has_content>: <#compress>
		<#local attachments = submission.allAttachments />
		<#local assignment = submission.assignment />
        <#local module = assignment.module />

		<#if attachments?size gt 0>
			<#if attachments?size == 1>
				<#local filename = "${attachments[0].name}">
			<#else>
				<#local filename = "submission-${submission.universityId}.zip">
			</#if>
			<a class="long-running" href="<@url page='/admin/module/${module.code}/assignments/${assignment.id}/submissions/download/${submission.id}/${filename}'/>"><#compress>
				${attachments?size}
				<#if attachments?size == 1> file
				<#else> files
				</#if>
			</#compress></a><#--
		--></#if><#--
		--><#if submission.submittedDate??> <#compress>
			<span class="date use-tooltip" title="<@lateness submission />" data-container="body"><#compress>
				<@fmt.date date=submission.submittedDate seconds=true capitalise=true shortMonth=true />
			</#compress></span>
		</#compress></#if><#--
		--><#if assignment.wordCountField?? && submission.valuesByFieldName[assignment.defaultWordCountName]??><#compress>
			, ${submission.valuesByFieldName[assignment.defaultWordCountName]?number} words
		</#compress></#if>
	</#compress></#if>
</#compress></#macro>

<#macro submission_status submission="" enhancedExtension="" enhancedFeedback="">
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
	<#elseif !enhancedFeedback?has_content>
		<span class="label label-important use-tooltip" title="<@lateness submission />" data-container="body">Late</span>
		<#if enhancedExtension?has_content>
			<#local extension=enhancedExtension.extension>
			<#if extension.approved && !extension.rejected>
				<#local date>
					<@fmt.date date=extension.expiryDate capitalise=true shortMonth=true />
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