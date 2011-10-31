<#escape x as x?html>

<h1>${module.code?upper_case} (${module.name})</h1>

<#if module.assignments?size gt 0>
<#list module.assignments as assignment>

	<div class="assignment">
		<h2>${assignment.name}</h2>
		<p>Here will be some actual information and the submission form.</p>
	</div>

</#list>
<#else>
<p>
	<!-- Sorry about this nonsense paragraph. -->
	This is where you would be able to submit coursework for this module if any courseworks had
	been set up here. If you think you have an assignment to submit for this module, the
	person setting the coursework may not yet be using this website to accept submissions and you
	should check with them where you should be submitting your coursework.
</p>
</#if>

</#escape>