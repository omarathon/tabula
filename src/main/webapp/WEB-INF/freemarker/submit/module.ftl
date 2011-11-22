<#assign fmt=JspTaglibs["/WEB-INF/tld/fmt.tld"]>
<#assign warwick=JspTaglibs["/WEB-INF/tld/warwick.tld"]>
<#escape x as x?html>

<h1>${module.code?upper_case} (${module.name})</h1>

<#if assignments?size gt 0>

<#list assignments as assignment>

	<div class="assignment ${assignment.active?string("active", "inactive")}">
		<h2>
		<#if assignment.active>
			<a href="${assignment.id}">${assignment.name}</a>
		<#else>
			${assignment.name}
		</#if>
		</h2>
		<div class="date end-date">
			Submission ${assignment.active?string("closes", "closed")} 
			<@warwick.formatDate value=assignment.closeDate pattern="d MMMM yyyy HH:mm:ss (z)" />
		</div>
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