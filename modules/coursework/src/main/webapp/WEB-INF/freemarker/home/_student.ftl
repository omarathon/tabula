<#assign has_feedback=assignmentsWithFeedback?has_content />
<#assign has_submissions=assignmentsWithSubmission?has_content />
<#assign has_assignments=enrolledAssignments?has_content />

<#assign has_any_items = (has_feedback || has_submissions || has_assignments) />

<#assign missing_assignments_markup>
	<p>Talk to your module convenor if this seems like a mistake.</p>
	<ul class="muted">
		<li>They may not have set up the assignment yet</li>
		<li>They may not be using Tabula for assessment</li>
		<li>You may not be correctly enrolled.</li>
	</ul>
</#assign>

<#if has_any_items || user.student>
	<h2>Your assignments</h2>
	
	<div class="row">
		<div class="span6">
			<h3>Pending</h3>
			
			<#if has_any_items>
				<ul class="links">		
					<#if has_assignments>
						<#macro enrolled_assignment info>
							<#local assignment = info.assignment />
							<#local extension = info.extension!false />
							<#local isExtended = info.isExtended!false />
							<#local extensionRequested = info.extensionRequested!false />
							<@fmt.assignment_link assignment />
							<#if info.submittable>
								<#include "../submit/assignment_deadline.ftl" />
							</#if>
						</#macro>
						<#list enrolledAssignments as info>
							<li class="assignment-info">
								<span class="label label-info">Enrolled</span>
								<@enrolled_assignment info />
							</li>
						</#list>
					</#if>

					<#if has_feedback>
						<#list assignmentsWithFeedback as assignment>
							<li class="assignment-info">
								<span class="label-green">Marked</span>
								<@fmt.assignment_link assignment />
							</li>
						</#list>
					</#if>

					<#if has_submissions>
						<#list assignmentsWithSubmission as assignment>
							<li class="assignment-info">
								<span class="label-orange">Submitted</span>
								<@fmt.assignment_link assignment />
							</li>
						</#list>
					</#if>
				</ul>	
			<#else><#-- !has_any_items -->
				<div class="alert alert-block alert-warning">
					<h4>We don't have anything for you here.</h4>
					${missing_assignments_markup}
				</div>		
			</#if>
			
			<#-- just as a footnote, really. Might need to recede more -->
			<small class="alert">
				<h4>Is an assignment missing here?</h4>
				${missing_assignments_markup}
			</small>
		</div>
		
		<div class="span6">
			<h3>Archive</h3>

			<#if archivedAssignments?has_content>
				<div id="archived-assignments-container">
					<ul class="links" id="archived-assignments-list">
						<#list archivedAssignments as assignment>
							<li class="assignment-info">
								<@assignment_link assignment>
									<@format_name assignment />	
								</@assignment_link>
							</li>
						</#list>
					</ul>
				</div>
			<#else>
				<p class="alert">There are no archived assignments to show you right now.</p>
			</#if>
		</div>
	</div>
</#if>