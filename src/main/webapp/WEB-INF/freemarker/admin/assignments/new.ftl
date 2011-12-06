<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>

<h1>Create assignment for <@fmt.module_name module /></h1>

<@f.form method="post" action="/admin/module/${module.code}/assignments/new" commandName="addAssignmentCommand">
<@f.errors cssClass="error form-errors">

</@f.errors>

<#include "_fields.ftl" />

<div class="submit-buttons actions">
<input type="submit" value="Create">
or <a href="/admin/department/${department.code}/#module-${module.code}">Cancel</a>
</div>

</@f.form>

</#escape>