<#if nonempty(examsForMarking?keys)>

	<#macro marker_info exam>

		<#if !exam.released>
			<div class="pull-right btn-group">
				<a class="btn btn-primary disabled use-tooltip" data-title="You'll be able to mark this exam when an administrator releases it for marking." data-container="body">Mark</a>
			</div>
		<#else>
			<#local marking = mapGet(examsForMarking, exam) />
			<#local needsMarking = marking._2()?size - marking._1()?size />
			<div class="pull-right btn-group">
				<a class="btn <#if (needsMarking > 0)>btn-primary</#if>" href="<@routes.examMarkerAddMarks exam=exam marker=user.apparentUser/>" data-container="body">Mark</a>
			</div>
			<#if (needsMarking > 0)>
				<span class="label label-warning"><@fmt.p needsMarking "student needs" "students need"/> marks adding</span>
			<#else>
				<span class="label label-success">All marks provided</span>
			</#if>
		</#if>

		<div class="clearfix"></div>
	</#macro>

	<h2 class="section">Exams for marking</h2>
	<p>You're a marker for one or more exams.</p>
	<div class="simple-assignment-list">
		<#list examsForMarking?keys as exam>
			<div class="simple-assignment-info">
				<@fmt.assignment_name exam />
				<@marker_info exam />
			</div>
		</#list>
	</div>
</#if>
