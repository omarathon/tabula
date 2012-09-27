<#assign has_feedback=assignmentsWithFeedback?has_content />
<#assign has_submissions=assignmentsWithSubmission?has_content />
<#assign has_assignments=enrolledAssignments?has_content />

<#if has_feedback || has_submissions || has_assignments>
<h2>Your assignments</h2>

<#macro format_name assignment>
	${assignment.module.code?upper_case} (${assignment.module.name}) - ${assignment.name}
</#macro>
<#macro format_assignment_link assignment>
	<a href="<@url page='/module/${assignment.module.code}/${assignment.id}/' />">
		<@format_name assignment />	
	</a>
</#macro>

<ul class="links">

<#if has_assignments>
<#list enrolledAssignments as assignment>
	<li class="assignment-info">
		<span class="label label-info">Enrolled</span>
		<@format_assignment_link assignment />
	</li>
</#list>
</#if>

<#if has_feedback>
<#list assignmentsWithFeedback as assignment>
	<li class="assignment-info">
		<span class="label-green">Marked</span>
		<@format_assignment_link assignment />
	</li>
</#list>
</#if>

<#if has_submissions>
<#list assignmentsWithSubmission as assignment>
	<li class="assignment-info">
		<span class="label-orange">Submitted</span>
		<@format_assignment_link assignment />
	</li>
</#list>
</#if>

</ul>
</#if>

<#if archivedAssignments?has_content>
<div id="archived-assignments-container">
<ul class="links" id="archived-assignments-list">
<#list archivedAssignments as assignment>
	<li class="assignment-info">
		<@format_assignment_link assignment />
	</li>
</#list>
</ul>
</div>
</#if>