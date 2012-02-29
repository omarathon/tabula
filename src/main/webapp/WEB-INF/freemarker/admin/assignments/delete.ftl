<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>


<@f.form method="post" action="/admin/module/${module.code}/assignments/${assignment.id}/delete" commandName="deleteAssignmentCommand">

<p></p>

<div class="submit-buttons actions">
<input type="submit" value="Delete">
or <a href="<@routes.depthome module=assignment.module />">Cancel</a>
</div>
</@f.form>

</#escape>