<#macro lateness submission><#compress>
	<#if submission?? && submission.submittedDate?? && (submission.late || submission.authorisedLate)>
		${durationFormatter(assignment.closeDate, submission.submittedDate)} after close
	</#if>
</#compress></#macro>

<#macro submission_details submission=""><#compress>
	<#if submission?has_content>: <#compress>
		<#local attachments = submission.allAttachments />
		<#local assignment = submission.assignment />
        <#local module = assignment.module />

		<#if attachments?size gt 0>
			<#if attachments?size == 1>
				<#local filename = "${submission.universityId} - ${attachments[0].name}">
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