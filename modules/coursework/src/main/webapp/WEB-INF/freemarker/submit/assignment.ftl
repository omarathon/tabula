
<#escape x as x?html>
<#compress>
<h1>${module.name} (${module.code?upper_case})
<br><strong>${assignment.name}</strong></h1>

<#if can.do("Assignment.Update", assignment)>
<div class="alert alert-info">
  <button type="button" class="close" data-dismiss="alert">×</button>
	<h3>Assignment page for students</h3>
	
	<p>You can give students a link to this page to 
		<#if assignment.collectSubmissions>submit work and to</#if> 
		receive their feedback<#if assignment.collectMarks> and/or marks</#if>.</p>
	   
	<p><a class="btn" href="<@routes.depthome module/>">Module management - ${assignment.module.code}</a></p>
</div>
</#if>

<a id="submittop"></a>

<#if feedback??>

	<#include "assignment_feedbackdownload.ftl" />
	<#if features.submissions>
		<#if submission??>
			<#include "assignment_submissionthanks.ftl" />
	    </#if>
	</#if>
    
<#else>

	<#if features.submissions>
		<#if submission??>
			<#include "assignment_submissionthanks.ftl" />
		</#if>
			
		<#-- At some point, also check if resubmission is allowed for this assignment -->
		<#include "assignment_submissionform.ftl" />
		
		<#if submission?? && !canReSubmit>
			<#if assignment.allowResubmission>
				<p>It is not possible to resubmit your assignment because the deadline has passed.</p>
			<#else>
				<p>This assignment does not allow you to resubmit.</p>
			</#if>
		</#if>
		
		
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
