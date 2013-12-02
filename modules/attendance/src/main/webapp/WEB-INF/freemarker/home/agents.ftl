<#escape x as x?html>
<#import "../attendance_macros.ftl" as attendance_macros />

<h1>${command.relationshipType.agentRole?capitalize}s</h1>

<#assign thisPath><@routes.viewDepartmentAgents command.department command.relationshipType /></#assign>
<@attendance_macros.academicYearSwitcher thisPath command.academicYear command.thisAcademicYear />

<div class="agent-search" style="display: none;">
	<div class="input-append">
		<input class="input-xlarge" type="text" placeholder="Search for a student or ${command.relationshipType.agentRole}&hellip;"/>
		<button class="btn"><i class="icon-search"></i></button>
	</div>
	<span class="muted" style="display: none;">Please enter at least 3 characters</span>
</div>

<table class="agents table table-bordered table-striped">
	<thead>
		<tr>
			<th>${command.relationshipType.agentRole?capitalize}s</th>
			<th>${command.relationshipType.studentRole?capitalize}s</th>
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
						${command.relationshipType.studentRole?capitalize}s:
						<#list agentData.students as student>
							<span class="name">${student.fullName}<span class="comma">, </span></span>
						</#list>
					</p>
				</td>
				<td class="students">
					<a class="use-popover" data-title="${command.relationshipType.studentRole?capitalize}s" data-content="${studentPopoverContent}" data-html="true">
						${agentData.students?size}
					</a>
				</td>
				<td class="unrecorded">
					<span class="badge badge-<#if (agentData.unrecorded > 2)>important<#elseif (agentData.unrecorded > 0)>warning<#else>success</#if>">
						${agentData.unrecorded}
					</span>
				</td>
				<td class="missed">
					<span class="badge badge-<#if (agentData.missed > 2)>important<#elseif (agentData.missed > 0)>warning<#else>success</#if>">
						${agentData.missed}
					</span>
				</td>
				<td class="button">
					<#if agentData.agentMember?? >
						<a href="<@routes.viewDepartmentAgentsStudents command.department command.relationshipType agentData.agentMember />" class="btn btn-primary">Attendance</a>
					<#else>
						&nbsp;
					</#if>
				</td>
			</tr>
		</#list>
	</tbody>
</table>

<script type="text/javascript">
	jQuery(function($){
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