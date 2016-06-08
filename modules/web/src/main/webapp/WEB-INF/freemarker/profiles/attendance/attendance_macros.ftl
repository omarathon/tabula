<#macro checkpointLabel department checkpoint="" point="" student="" note="">
	<#local formatResult = formatResult(department, checkpoint, point, student, note) />
	<#local popoverContent>
		<#if formatResult.status?has_content><p>${formatResult.status}</p></#if>
		<#if formatResult.metadata?has_content><p>${formatResult.metadata}</p></#if>
		<#if formatResult.noteType?has_content><p>${formatResult.noteType}</p></#if>
		<#if formatResult.noteText?has_content><p>${formatResult.noteText}</p></#if>
		<#if formatResult.noteUrl?has_content><p><a class='attendance-note-modal' href='<@routes.profiles.view_mointeringpoint_attendance_note checkpoint/>'>View attendance note</a></p></#if>
	</#local>
	<span class="use-popover label ${formatResult.labelClass}" data-content="${popoverContent}" data-html="true" data-placement="left">${formatResult.labelText}</span>
	<span class="visible-print">
		<#if formatResult.metadata?has_content>${formatResult.metadata}<br /></#if>
		<#if formatResult.noteType?has_content>${formatResult.noteType}<br /></#if>
		<#if formatResult.noteText?has_content>${formatResult.noteText}</#if>
	</span>

</#macro>

<#macro checkpointDescription department checkpoint="" point="" student="" note="">
	<#local formatResult = formatResult(department, checkpoint, point, student, note) />
	<#if formatResult.metadata?has_content><p>${formatResult.metadata}</p></#if>
</#macro>

<#function formatResult department checkpoint="" point="" student="" note="">
	<#if checkpoint?has_content>
		<#if note?has_content>
			<#return attendanceMonitoringCheckpointFormatter(department, checkpoint, note) />
		<#else>
			<#return attendanceMonitoringCheckpointFormatter(department, checkpoint) />
		</#if>
	<#else>
		<#if note?has_content>
			<#return attendanceMonitoringCheckpointFormatter(department, point, student, note) />
		<#else>
			<#return attendanceMonitoringCheckpointFormatter(department, point, student) />
		</#if>
	</#if>
</#function>

