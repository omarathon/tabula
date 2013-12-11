<#--
	Used in /WEB-INF/freemarker/home/_student.ftl and assignment_submissionform.ftl
-->
<#if !assignment.openEnded>
	<#macro extensionButtonContents label assignment>
		<a href="<@routes.extensionRequest assignment=assignment />?returnTo=<@routes.assignment assignment=assignment />" class="btn btn-mini">
			<i class="icon-calendar"></i> ${label}
		</a>
	</#macro>

	<#macro extensionButton extensionRequested isExtended isClosed>
		<p class="extension-button">
			<#if extensionRequested>
				<@extensionButtonContents "Review extension request" assignment />
			<#elseif !isExtended && !isClosed && assignment.module.department.canRequestExtension>
				<@extensionButtonContents "Request an extension" assignment />
			</#if>
		</p>
	</#macro>

	<#assign time_remaining = durationFormatter(assignment.closeDate) />
	<#assign showIconsAndButtons = (!textOnly)!true />
	<#if hasExtension && isExtended>
		<#assign extension_time_remaining = durationFormatter(extension.expiryDate) />
	</#if>

	<#if isExtended>
		<p class="extended deadline">
			<#if showIconsAndButtons><i class="icon-calendar icon-3x pull-left"></i></#if>
			<span class="time-remaining">${extension_time_remaining} <span class="label label-info">Extended</span></span>
			Extension granted until <@fmt.date date=extension.expiryDate />
		</p>
		<#if showIconsAndButtons><@extensionButton extensionRequested isExtended assignment.closed /></#if>
	<#elseif assignment.closed>
		<p class="late deadline">
			<#if showIconsAndButtons><i class="icon-calendar icon-3x pull-left"></i></#if>
			<#if hasExtension && isExtended>
				<span class="time-remaining">${extension_time_remaining} <span class="label label-important">Late</span></span>
				Extension deadline was <@fmt.date date=extension.expiryDate />
			<#else>
				<span class="time-remaining">${time_remaining} <span class="label label-important">Late</span></span>
				Deadline was <@fmt.date date=assignment.closeDate />
			</#if>
		</p>
		<#if showIconsAndButtons><@extensionButton extensionRequested isExtended assignment.closed /></#if>
	<#else>
		<p class="deadline">
			<#if showIconsAndButtons><i class="icon-calendar icon-3x pull-left"></i></#if>
			<span class="time-remaining">${time_remaining}</span>
			Deadline <@fmt.date date=assignment.closeDate />
		</p>
		<#if showIconsAndButtons><@extensionButton extensionRequested isExtended assignment.closed /></#if>
	</#if>
</#if>