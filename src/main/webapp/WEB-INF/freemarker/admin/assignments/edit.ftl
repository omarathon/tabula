<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>

<@f.form method="post" action="/admin/module/${module.code}/assignments/edit/${assignment.id}" commandName="editAssignment">
<@f.errors cssClass="error form-errors">
</@f.errors>

<#include "_fields.ftl" />

<input type="submit" value="Save">
</@f.form>

</#escape>