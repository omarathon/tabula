<#--
	Used in /WEB-INF/freemarker/home/_student.ftl
-->
<#if !assignment.openEnded>
	<#macro extensionButtonContents label assignment>
		<a href="<@routes.extensionRequest assignment=assignment />" class="btn btn-mini">
			<i class="icon-calendar"></i> ${label}
		</a>
	</#macro>

	<#macro extensionButton extensionRequested isExtended isClosed>
		<#if willShowButtons>
			<#if extensionRequested>
				<@extensionButtonContents "Review extension request" assignment />
			<#elseif !isExtended && !isClosed>
				<@extensionButtonContents "Request an extension" assignment />
			</#if>
		</#if>
	</#macro>

	<#assign time_remaining = durationFormatter(assignment.closeDate) />
	<#assign willShowButtons = showButtons!true />
	<#if hasExtension && isExtended>
		<#assign extension_time_remaining = durationFormatter(extension.expiryDate) />
	</#if>

	<#if isExtended>
		<p class="extended deadline">
			<span class="time-remaining">${extension_time_remaining} <span class="label label-info">Extended</span></span>
			Extension granted until <@fmt.date date=extension.expiryDate />
		</p>
		<@extensionButton extensionRequested isExtended assignment.closed />
	<#elseif assignment.closed>
		<p class="late deadline">
			<#if hasExtension && isExtended>
				<span class="time-remaining">${extension_time_remaining} <span class="label label-important">Late</span></span>
				Extension deadline was <@fmt.date date=extension.expiryDate />
			<#else>
				<span class="time-remaining">${time_remaining} <span class="label label-important">Late</span></span>
				Deadline was <@fmt.date date=assignment.closeDate />
			</#if>
		</p>
		<@extensionButton extensionRequested isExtended assignment.closed />
	<#else>
		<p class="deadline">
			<span class="time-remaining">${time_remaining}</span>
			Deadline <@fmt.date date=assignment.closeDate />
		</p>
		<@extensionButton extensionRequested isExtended assignment.closed />
	</#if>
</#if>