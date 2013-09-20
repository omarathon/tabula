<#if nonempty(assignmentsForMarking)>

<#macro marker_info info>
	<#local assignment = info.assignment />
	<#local numSubmissions = info.numSubmissions!0 />
	<#assign time_remaining=durationFormatter(assignment.closeDate) />
	<#if numSubmissions==0>
		<#local class="disabled use-tooltip" />
		<#local href="" />
		<#local title>
			You'll be able to download submissions for marking when an administrator releases them.
		</#local>
	<#else>
		<#local class="" />
		<#local title="" />
		<#local href>
			<@routes.listmarkersubmissions assignment=assignment />
		</#local>
	</#if>
	<#if assignment.closed>
		<div class="alert alert-success deadline">
			Assignment closed: <strong><@fmt.date date=assignment.closeDate /> (${time_remaining})</strong>
			<div class="marker-btn btn-group">
				<a class="btn btn-mini ${class}" href="${href}" data-title="${title}" data-container="body">Manage <i class="icon-cog"></i></a>
			</div>
		</div>
	<#else>
		<div class="alert alert-info deadline">
			Assignment closes <strong><@fmt.date date=assignment.closeDate /> (${time_remaining})</strong>
		</div>
	</#if>
</#macro>

<h2 class="section">Assignments for marking</h2>
<p>You're a marker for one or more assignments.</p>
<div class="simple-assignment-list">
	<#list assignmentsForMarking as info>
		<div class="simple-assignment-info">
			<#if info.isAdmin>
				<@fmt.admin_assignment_link info.assignment />
			<#else>
				<@fmt.assignment_name info.assignment />
			</#if>
			<@marker_info info />
		</div>
	</#list>
</div>
<script>
	jQuery("a.disabled").on('click', function(e){e.preventDefault(e)})
</script>
</#if>
