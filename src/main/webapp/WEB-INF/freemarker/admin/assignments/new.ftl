<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>

<h1>Create assignment for <@fmt.module_name module /></h1>

<@f.form method="post" action="/admin/module/${module.code}/assignments/new" commandName="addAssignmentCommand">
<@f.errors cssClass="error form-errors">

</@f.errors>

<#assign newRecord=true />

<#include "_fields.ftl" />

<div class="submit-buttons actions">
<input type="submit" value="Create">
or <a href="<@routes.depthome module=module />">Cancel</a>
</div>

</@f.form>

</#escape>