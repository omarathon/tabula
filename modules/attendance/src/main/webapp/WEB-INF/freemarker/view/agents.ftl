<#escape x as x?html>

<h1>${relationshipType.agentRole?capitalize}s</h1>

<div class="agent-search" style="display: none;">
	<div class="input-append">
		<input class="input-xlarge" type="text" placeholder="Search for a student or ${relationshipType.agentRole}&hellip;"/>
		<button class="btn"><i class="icon-search"></i></button>
	</div>
	<span class="muted" style="display: none;">Please enter at least 3 characters</span>
</div>

<div class="fix-area">
<table class="agents table table-bordered table-striped">
	<thead class="fix-header pad-when-fixed">
		<tr>
			<th>${relationshipType.agentRole?capitalize}s</th>
			<th>${relationshipType.studentRole?capitalize}s</th>
			<th>Unrecorded</th>
			<th>Missed</th>
			<th></th>
		</tr>
	</thead>
	<tbody>
		<#list agents as agentData>
			<#assign studentPopoverContent>
				<ul>
					<#list agentData.students as student>
						<li>${student.fullName}</li>
					</#list>
				</ul>
			</#assign>
			<tr>
				<td class="agent" data-sortby="<#if agentData.agentMember??>${agentData.agentMember.lastName}, ${agentData.agentMember.firstName}<#else>${agentData.agent}</#if>">
					<h6><#if agentData.agentMember??>${agentData.agentMember.fullName}<#else>${agentData.agent}</#if></h6>
					<p class="student-list" style="display: none;">
						${relationshipType.studentRole?capitalize}s:
						<#list agentData.students as student>
							<span class="name">${student.fullName}<span class="comma">, </span></span>
						</#list>
					</p>
				</td>
				<td class="students">
					<a class="use-popover" data-title="${relationshipType.studentRole?capitalize}s" data-content="${studentPopoverContent}" data-html="true">
						${agentData.students?size}
					</a>
				</td>
				<td class="unrecorded">
					<span class="badge badge-<#if (agentData.unrecordedCount > 2)>important<#elseif (agentData.unrecordedCount > 0)>warning<#else>success</#if>">
						${agentData.unrecordedCount}
					</span>
				</td>
				<td class="missed">
					<span class="badge badge-<#if (agentData.missedCount > 2)>important<#elseif (agentData.missedCount > 0)>warning<#else>success</#if>">
						${agentData.missedCount}
					</span>
				</td>
				<td class="button">
					<#if agentData.agentMember?? >
						<a href="<@routes.viewAgent department academicYear.startYear?c relationshipType agentData.agentMember />" class="btn btn-primary">Attendance</a>
					<#else>
						&nbsp;
					</#if>
				</td>
			</tr>
		</#list>
	</tbody>
</table>
</div>

<script type="text/javascript">
	jQuery(function($){
		$('.fix-area').fixHeaderFooter();

		$('table.agents').tablesorter({
			sortList: [[0,0]],
			headers: { 4: { sorter: false }},
			textExtraction: function(node) {
				var $el = $(node);
				if ($el.data('sortby')) {
					return $el.data('sortby');
				} else {
					return $el.text().trim();
				}
			}
		});
	});
</script>

</#escape>