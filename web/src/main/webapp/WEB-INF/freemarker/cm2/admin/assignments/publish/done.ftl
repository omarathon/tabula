<#import "*/cm2_macros.ftl" as cm2 />
<#escape x as x?html>
	<@cm2.assignmentHeader "Published feedback" assignment "for" />

	<p>
		The feedback has been published.
		Students will be able to access their feedback by visiting this page:
	</p>

	<p>
		<a href="<@routes.cm2.assignment assignment />"><@routes.cm2.assignment assignment /></a>
	</p>
</#escape>