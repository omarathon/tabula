<#if nonempty(assignmentsForMarking)>

<#macro format_name assignment>
	${assignment.module.code?upper_case} (${assignment.module.name}) - ${assignment.name}
</#macro>

<#macro download_link info>
	<#local assignment = info.assignment />
	<#local numSubmissions = info.numSubmissions!0 />
	<#assign time_remaining=durationFormatter(assignment.closeDate) />
	<#if numSubmissions==0>
		<#assign class="disabled use-tooltip" />
		<#assign href="" />
		<#assign title>
			No submissions have been released to you yet. When the administrator releases submissions for marking you
			will be able to download them here.
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

<h2>Assignments for marking</h2>
<p>You're a marker for one or more assignments.</p>
<ul class="links">
	<#list assignmentsForMarking as info>
		<li class="assignment-info">
			<@format_name info.assignment />
			<@download_link info />
		</li>
	</#list>
</ul>
<script>
	jQuery("a.disabled").on('click', function(e){e.preventDefault(e)})
</script>
</#if>