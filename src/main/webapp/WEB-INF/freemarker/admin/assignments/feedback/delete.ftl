<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>


<@f.form method="post" action="/admin/module/${module.code}/assignments/${assignment.id}/feedback/delete" commandName="deleteFeedbackCommand">

<h1>Delete feedback for ${assignment.name}</h1>

<@form.errors path="" />

<input type="hidden" name="confirmScreen" value="true" />

<@spring.bind path="feedbacks">
<@form.errors path="feedbacks" />
<#assign feedbacksList=status.actualValue />
<p>Deleting <strong><@fmt.p feedbacksList?size "feedback item" /></strong>.
You only need to do this if the wrong feedback has been uploaded. If you are trying to re-use this assignment,
you should go back and create a separate assignment instead.
</p>
<#list feedbacksList as feedback>
<input type="hidden" name="feedbacks" value="${feedback.id}" />
</#list>
</@spring.bind>

<p>
<@form.errors path="confirm" />
<label><@f.checkbox path="confirm" /> I confirm that I want to permanently delete these feedback items.</label> 
</p>

<div class="submit-buttons">
<input class="btn btn-danger" type="submit" value="Delete">
</div>
</@f.form>

</#escape>