<#assign has_feedback = assignmentsWithFeedback?has_content />
<#assign has_submissions = assignmentsWithSubmission?has_content />
<#assign has_assignments = enrolledAssignments?has_content />
<#assign has_archived = archivedAssignments?has_content />

<#assign has_pending_items = (has_feedback || has_assignments) />
<#assign has_historical_items = (has_submissions || has_archived || has_feedback) />

<#assign missing_assignments_markup>
	<p>Talk to your module convenor if this seems like a mistake.</p>
	<ul>
		<li>They may not have set up the assignment yet</li>
		<li>They may not be using Tabula for assessment</li>
		<li>You may not be correctly enrolled.</li>
	</ul>
</#assign>

<#if has_pending_items || has_historical_items || user.student>
	<h2 class="section">Your assignments</h2>
	
	<div class="row-fluid">
		<div class="span6">
			<h6>Pending</h6>
			
			<#if has_pending_items>
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
							<li class="simple-assignment-info">
								<#if !info.isExtended && info.closed>
									<span class="pull-right label label-important">Late</span>
								<#elseif info.isExtended!false>
									<span class="pull-right label label-info">Extended</span>
								</#if>
								<@enrolled_assignment info />
							</li>
						</#list>
					</#if>
				</ul>	
			
				<div class="alert alert-block">
					<h6>Is an assignment missing here?</h6>
					${missing_assignments_markup}
				</div>
			<#else>
				<div class="alert alert-block">
					<h6>We don't have anything for you here.</h6>
					${missing_assignments_markup}
				</div>
			</#if>
		</div>
		
		<div class="span6">
			<h6>Past</h6>

			<#if has_historical_items>
				<#if has_submissions || has_feedback>
					<ul class="links" id="submitted-assignments-list">
						<#list assignmentsWithFeedback as assignment>
							<li class="simple-assignment-info">
								<span class="pull-right label label-success">Marked</span>
								<@fmt.assignment_link assignment />
							</li>
						</#list>
						
						<#list assignmentsWithSubmission as assignment>
							<li class="simple-assignment-info">
								<span class="pull-right label">Submitted</span>
								<@fmt.assignment_link assignment />
							</li>
						</#list>
					</ul>
				</#if>
	
				<#if has_archived>
					<div id="archived-assignments-container">
						<ul class="links" id="archived-assignments-list">
							<#list archivedAssignments as assignment>
								<li class="simple-assignment-info">
									<span class="pull-right label">Archived</span>
									<@fmt.assignment_link assignment />
								</li>
							</#list>
						</ul>
					</div>
				</#if>
			<#else>
				<p class="alert">There are no old assignments to show you right now.</p>
			</#if>
		</div>
	</div>
</#if>