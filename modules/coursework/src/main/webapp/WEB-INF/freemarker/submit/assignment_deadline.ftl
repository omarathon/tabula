<#--

  Referenced by home/_student

-->
<#assign time_remaining=durationFormatter(assignment.closeDate) />
<#if isExtended>
    <#assign extension_time_remaining=durationFormatter(extension.expiryDate) />
    <div class="alert alert-info">
        You have been granted an extension for this assignment.
        Extended submission deadline: <strong><@fmt.date date=extension.expiryDate timezone=true /></strong> (${extension_time_remaining})
    </div>
<#elseif assignment.closed>
	<div class="alert alert-error">
		Submission deadline: <strong><@fmt.date date=assignment.closeDate timezone=true /> (${time_remaining})</strong>
	</div>
<#else>
	<div class="alert alert-info">
		Submission deadline: <strong><@fmt.date date=assignment.closeDate timezone=true /></strong> (${time_remaining})
	</div>
</#if>