<#if nonempty(assignmentsForMarking)>

<#macro download_link info>
	<#local assignment = info.assignment />
	<#local numSubmissions = info.numSubmissions!0 />
	<#assign time_remaining=durationFormatter(assignment.closeDate) />
	<#if numSubmissions==0>
		<#assign class="disabled use-tooltip" />
		<#assign href="" />
		<#assign title>
			You'll be able to download submissions for marking when an administrator releases them.
		</#assign>
	<#else>
		<#assign class="" />
		<#assign title="" />
		<#assign href>
			<@routes.downloadmarkersubmissions assignment=assignment />
		</#assign>
	</#if>
	<#if assignment.closed>
		<div class="alert alert-success deadline">
			Assignment closed: <strong><@fmt.date date=assignment.closeDate timezone=true /> (${time_remaining})</strong>
			<a href="${href}" class="btn btn-mini ${class}" data-title="${title}">
				<i class="icon-download"></i> Download submissions (${numSubmissions})
			</a>
		</div>
	<#else>
		<div class="alert alert-info deadline">
			Assignment closes on <strong><@fmt.date date=assignment.closeDate timezone=true /> (${time_remaining})</strong>
		</div>
	</#if>
</#macro>

<h2 class="section">Assignments for marking</h2>
<p>You're a marker for one or more assignments.</p>
<div class="simple-assignment-list">
	<#list assignmentsForMarking as info>
		<div class="simple-assignment-info">
			<@fmt.assignment_link info.assignment />
			<@download_link info />
		</div>
	</#list>
</div>
<script>
	jQuery("a.disabled").on('click', function(e){e.preventDefault(e)})
</script>
</#if>
