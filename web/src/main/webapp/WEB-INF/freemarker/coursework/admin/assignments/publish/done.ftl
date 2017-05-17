<#escape x as x?html>

<h1>Published feedback for ${assignment.name}.</h1>

<#assign module=assignment.module />
<#assign department=module.adminDepartment />

<p>
The feedback has been published.
Students will be able to access their feedback by visiting this page:
</p>

<p>
<#assign feedbackUrl><@routes.coursework.assignment assignment /></#assign>
<a href="${feedbackUrl}">
${feedbackUrl}
</a>
</p>

<p>
<a href="<@routes.coursework.depthome module />">
Return to assignment info
</a>
</p>

</#escape>