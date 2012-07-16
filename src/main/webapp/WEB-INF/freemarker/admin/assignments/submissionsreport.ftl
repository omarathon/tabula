<#compress>
<#escape x as x?html>

<#if !embedded>
<h1>Submissions/feedback comparison for ${assignment.name}</h1>
</#if>

<#if report.hasProblems>

	<#if submissionOnly?size gt 0>
	<div class="alert alert-error">
		<p>These users have submitted to the assignment, but no feedback has been uploaded for them.</p>
		
		<ul class="user-list">
		<#list submissionOnly as u>
			<li>${u.warwickId}</li>
		</#list>
		</ul>
	</div>
	</#if>
	
	<#if feedbackOnly?size gt 0>
	<div class="alert alert-error">
		<p><i class="icon-remove"></i> There is feedback for these users but they did not submit an assignment to this system.</p>
		
		<ul class="user-list">
		<#list feedbackOnly as u>
			<li>${u.warwickId}</li>
		</#list>
		</ul>
	</div>
	</#if>
	
	<#if hasNoAttachments?size == assignment.feedbacks?size>
		<div class="alert alert-warn">
			<p><i class="icon-remove"></i> None of the submissions have any feedback.</p>
		</div>
	<#elseif hasNoAttachments?size gt 0>
		<div class="alert alert-error">
			<p><i class="icon-remove"></i> Submissions received from the following students do not have any feedback.</p>
			<ul class="user-list">
			<#list hasNoAttachments as u>
				<li>${u.warwickId}</li>
			</#list>
			</ul>
		</div>	
	</#if>

	<#if hasNoMarks?size == assignment.feedbacks?size>
		<div class="alert alert-warn">
			<p><i class="icon-remove"></i> None of the submissions have had marks assigned.</p>
		</div>
	<#elseif hasNoMarks?size gt 0>
		<div class="alert alert-error">
			<p><i class="icon-remove"></i> Submissions received from the following students do not have any marks assigned.</p>
			<ul class="user-list">
			<#list hasNoMarks as u>
				<li>${u.warwickId}</li>
			</#list>
			</ul>
		</div>	
	</#if>

	<p>
		The above discrepencies are provided for information.
		It is up to you to decide whether to continue publishing. 
	</p>
	

<#else>
	<div class="alert alert-success">
	<p><i class="icon-ok"></i> The submissions and the feedback items appear to match up for this assignment.</p>
	</div>
</#if>

</#escape>
</#compress>