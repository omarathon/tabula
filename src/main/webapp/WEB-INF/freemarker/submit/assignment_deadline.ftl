<#--

  Referenced by home/_student

-->
<#assign time_remaining=durationFormatter(assignment.closeDate) />

<#macro extensionButton label assignment>
	<a href="<@routes.extensionRequest assignment=assignment />" class="btn btn-mini">
		<i class="icon-calendar"></i> ${label}
	</a>
</#macro>

<#if isExtended>
	<#assign extension_time_remaining=durationFormatter(extension.expiryDate) />
	<div class="alert alert-info deadline">
		You have been granted an extension for this assignment.
		Extended submission deadline: <strong><@fmt.date date=extension.expiryDate timezone=true /></strong> (${extension_time_remaining})
		<#if extensionRequested>
			<@extensionButton "Review extension request" assignment/>
		</#if>
	</div>
<#elseif assignment.closed>
	<div class="alert alert-error deadline">
		Submission deadline: <strong><@fmt.date date=assignment.closeDate timezone=true /> (${time_remaining})</strong>
		<#if extensionRequested>
			<@extensionButton "Review extension request" assignment/>
		</#if>
	</div>
<#else>
	<div class="alert alert-info deadline">
		Submission deadline: <strong><@fmt.date date=assignment.closeDate timezone=true /></strong> (${time_remaining})
		<#if assignment.module.department.allowExtensionRequests!false && assignment.allowExtensions!false>
			<#if extensionRequested>
				<@extensionButton "Review extension request" assignment/>
			<#else>
				<@extensionButton "Request an extension" assignment/>
			</#if>
		</#if>
	</div>
</#if>