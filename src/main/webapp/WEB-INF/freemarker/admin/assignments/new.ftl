<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>

<h1>Create a new assignment for ${module.code?upper_case}</h1>

<@f.form method="post" action="/admin/module/${module.code}/assignments/new" commandName="addAssignment">
<@f.errors cssClass="error form-errors">

</@f.errors>

<#include "_fields.ftl" />

<input type="submit" value="Create">

or <a href="/admin/department/${department.code}/#module-${module.code}">Cancel</a>

</@f.form>

</#escape>