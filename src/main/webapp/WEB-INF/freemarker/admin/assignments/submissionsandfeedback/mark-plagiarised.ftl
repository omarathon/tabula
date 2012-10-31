<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>



<@f.form method="post" action="/admin/module/${module.code}/assignments/${assignment.id}/submissionsandfeedback/mark-plagiarised" commandName="markPlagiarisedCommand">


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

<@spring.bind path="students">
<@form.errors path="students" />
<#assign students=status.actualValue />
<p>
${verb?cap_first}ing <strong><@fmt.p students?size "student" /></strong> as suspected of being plagiarised.
${message}
</p>
<#list students as student>
<input type="hidden" name="students" value="${student.uniId}" />
</#list>
</@spring.bind>

<@spring.bind path="markPlagiarised">
<input type="hidden" name="markPlagiarised" value="${status.value}" />
</@spring.bind>

<p>
<@form.errors path="confirm" />
<@form.label checkbox=true><@f.checkbox path="confirm" />
<#if (submissionsList?size > 1)>
 I confirm that I want to ${verb} these submissions as suspected of being plagiarised.
<#else>
 I confirm that I want to ${verb} this submission as suspected of being plagiarised.
</#if>
 </label>
 </@form.label> 
</p>

<div class="submit-buttons">
<input class="btn btn-warn" type="submit" value="Confirm">
</div>
</@f.form>

</#escape>