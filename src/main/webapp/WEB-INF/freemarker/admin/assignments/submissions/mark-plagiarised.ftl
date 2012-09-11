<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>



<@f.form method="post" action="/admin/module/${module.code}/assignments/${assignment.id}/submissions/mark-plagiarised" commandName="markPlagiarisedCommand">


<#if markPlagiarisedCommand.markPlagiarised> 
	<#assign verb = "mark">
	<#assign message = "Items marked in this way will be held back when feedback is published.">
	
<#else>
	<#assign verb = "unmark">
	<#assign message = "Items will be included when feedback is published.">

</#if>


<h1>Suspected plagiarism for ${assignment.name}</h1>

<@form.errors path="" />

<input type="hidden" name="confirmScreen" value="true" />

<@spring.bind path="submissions">
<@form.errors path="submissions" />
<#assign submissionsList=status.actualValue />
<p>
${verb?cap_first}ing <strong><@fmt.p submissionsList?size "submission item" /></strong> as suspected plagiarised.
${message}
</p>
<#list submissionsList as submission>
<input type="hidden" name="submissions" value="${submission.id}" />
</#list>
</@spring.bind>

<@spring.bind path="markPlagiarised">
<input type="hidden" name="markPlagiarised" value="${status.value}" />
</@spring.bind>

<p>
<@form.errors path="confirm" />
<@form.label checkbox=true><@f.checkbox path="confirm" /> I confirm that I want to ${verb} these submission items as suspected plagiarised.</label></@form.label> 
</p>

<div class="submit-buttons">
<input class="btn btn-warn" type="submit" value="Confirm">
</div>
</@f.form>

</#escape>