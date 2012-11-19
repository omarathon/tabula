<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>


<@f.form method="post" action="${url('/admin/module/${module.code}/assignments/${assignment.id}/submissions/delete')}" commandName="deleteSubmissionCommand">

<h1>Delete submissions for ${assignment.name}</h1>

<@form.errors path="" />

<input type="hidden" name="confirmScreen" value="true" />

<@spring.bind path="submissions">
<@form.errors path="submissions" />
<#assign submissionsList=status.actualValue />
<p>
Deleting <strong><@fmt.p submissionsList?size "submission item" /></strong>.
You only need to do this if an erroneous submission has been made. If you are trying to delete submissions to
re-use this assignment, you should instead go back and create a separate assignment.
</p>
<#list submissionsList as submission>
<input type="hidden" name="submissions" value="${submission.id}" />
</#list>
</@spring.bind>

<p>
<@form.errors path="confirm" />
<@form.label checkbox=true><@f.checkbox path="confirm" /> I confirm that I want to permanently delete these submission items.</label></@form.label> 
</p>

<div class="submit-buttons">
<input class="btn btn-danger" type="submit" value="Delete">
</div>
</@f.form>

</#escape>