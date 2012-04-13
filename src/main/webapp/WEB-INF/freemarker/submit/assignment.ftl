
<#escape x as x?html>
<#compress>
<h1>${module.name} (${module.code?upper_case})
<br><strong>${assignment.name}</strong></h1>

<#if feedback??>

	<#include "assignment_feedbackdownload.ftl" />

<#else>

	<#if features.submissions>
		<#if submission??>
			<#include "assignment_submissionthanks.ftl" />
		</#if>
			
		<#-- At some point, also check if resubmission is allowed for this assignment -->
		<#if !submission??>
			<#include "assignment_submissionform.ftl" />
		</#if><#-- submission?? -->
		
	<#else>
	
		<h2>${user.fullName} (${user.universityId})</h2>
		<p>
			If you've submitted your assignment, you should be able to access your
			feedback here once it's ready.
		</p>	
	
	</#if>

</#if>

</#compress>
</#escape>