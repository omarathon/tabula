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
<p>It seems that you're on this module, but no assignments have been set up here.</p>
	
<p>See your module tutor if you have an assignment to submit and aren't sure how you
should be submitting it.</p>
</#if>

</#escape>