<#if nonempty(examsForMarking)>

<#macro marker_info exam>

	<#if !exam.released>
		<#local class="disabled use-tooltip" />
		<#local href="" />
		<#local title>
			You'll be able to mark this exam when an administrator releases it for marking.
		</#local>
	<#else>
		<#local class="" />
		<#local title="" />
		<#local href>
			<@routes.examMarkerAddMarks exam=exam marker=user.apparentUser/>
		</#local>
	</#if>
	<div class="pull-right btn-group">
		<a class="btn btn-primary ${class}" href="${href}" data-title="${title}" data-container="body">Mark</a>
	</div>
	<div class="clearfix"></div>
</#macro>

<h2 class="section">Exams for marking</h2>
<p>You're a marker for one or more exams.</p>
<div class="simple-assignment-list">
	<#list examsForMarking as exam>
		<div class="simple-assignment-info">
				<@fmt.assignment_name exam />
			<@marker_info exam />
		</div>
	</#list>
</div>
<script>
	jQuery("a.disabled").on('click', function(e){e.preventDefault(e)})
</script>
</#if>
