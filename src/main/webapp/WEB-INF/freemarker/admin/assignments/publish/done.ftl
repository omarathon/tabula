<h1>Published feedback for ${assignment.name}.</h1>

<#assign module=assignment.module />
<#assign department=module.department />

<p>
The feedback has been published.
Students will be able to access their feedback by visiting this page:
</p>

<p>
Assignment page: 
<#assign feedbackUrl><@url page="/module/${module.code}/${assignment.id}"/></#assign>
<a href="${feedbackUrl}">
${feedbackUrl}
</a>
</p>

<p>They haven't been notified of this automatically, so you'll want to
distribute this link to them.</p>

<p>
<a href="<@url page="/admin/department/${department.code}/#module-${module.code}" />">
Return to assignment info
</a>
</p>