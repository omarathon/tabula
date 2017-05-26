<#import "*/cm2_macros.ftl" as cm2 />
<#escape x as x?html>
<#compress>
	<#if isReply>
		<@cm2.assignmentHeader "You have replied to a request for more information" assignment "for" />

		<p>
			You will receive an email when your reply has been reviewed. If your request for an extension has not been
			approved before the deadline, hand in any work that you have completed before the deadline passes.
		</p>
	<#else>
		<@cm2.assignmentHeader "You have requested an extension" assignment "for" />

		<p>
			You will receive an email when your request has been reviewed. If your request for an extension has not been
			approved before the deadline, hand in any work that you have completed before the deadline passes.
	</p>
	</#if>
	<p>
		If your circumstances change and you wish to provide additional information then you can edit your request by
		revisiting the submission page and pressing the button to review your request.
	</p>
	<a href="<@routes.cm2.assignment assignment=assignment />">Back to ${assignment.name}</a>
</#compress>
</#escape>