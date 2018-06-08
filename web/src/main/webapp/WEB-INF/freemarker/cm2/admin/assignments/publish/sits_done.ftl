<#import "*/cm2_macros.ftl" as cm2 />
<#escape x as x?html>
	<@cm2.assignmentHeader "Upload feedback to SITS" assignment "for" />

	<p>
		The feedback has been queued for upload to SITS.
	</p>

	<p>
		<a class="btn btn-default" href="<@routes.cm2.assignmentsubmissionsandfeedbacksummary assignment />">Return to submissions and feedback</a>
	</p>
</#escape>