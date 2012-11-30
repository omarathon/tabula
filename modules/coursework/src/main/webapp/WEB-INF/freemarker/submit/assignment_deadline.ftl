<#--

  Referenced by home/_student

-->
<#if !assignment.openEnded>
	<#assign time_remaining = durationFormatter(assignment.closeDate) />

	<#macro extensionButton label assignment>
		<a href="<@routes.extensionRequest assignment=assignment />" class="btn btn-mini">
			<i class="icon-calendar"></i> ${label}
		</a>
	</#macro>

	<#if isExtended>
		<#assign extension_time_remaining = durationFormatter(extension.expiryDate) />
		
		<p class="text-info deadline">Extension granted until <@fmt.date date=extension.expiryDate timezone=true /></strong> (${extension_time_remaining})</p>
		<#if extensionRequested>
			<@extensionButton "Review extension request" assignment />
		</#if>
	<#elseif assignment.closed>
		<p class="text-error deadline">Deadline was <@fmt.date date=assignment.closeDate timezone=true /></strong> (${time_remaining})</p>
		<#if extensionRequested>
			<@extensionButton "Review extension request" assignment />
		</#if>
	<#else>
		<p class="deadline">Deadline <@fmt.date date=assignment.closeDate timezone=true /></strong> (${time_remaining})</p>
		<#if assignment.module.department.allowExtensionRequests!false && assignment.allowExtensions!false>
			<#if extensionRequested>
				<@extensionButton "Review extension request" assignment />
			<#else>
				<@extensionButton "Request an extension" assignment />
			</#if>
		</#if>
	</#if>
</#if>