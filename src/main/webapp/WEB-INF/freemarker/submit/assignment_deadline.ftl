<#assign time_remaining=durationFormatter(assignment.closeDate) />
<#if assignment.closed>
	<div class="alert alert-error">
		Submission deadline: <strong><@fmt.date date=assignment.closeDate timezone=true /> (${time_remaining})</strong>
	</div>
<#else>
	<div class="alert alert-info">
		Submission deadline: <strong><@fmt.date date=assignment.closeDate timezone=true /></strong> (${time_remaining})
	</div>
</#if>