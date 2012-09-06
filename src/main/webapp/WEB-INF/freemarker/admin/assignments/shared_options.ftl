<#--

	Form shown in a modal popup to

	This is currently also included by _fields.ftl since the forms to add/edit a single assignment
	use the same fields.

-->
<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>

<@f.form method="post" action="/admin/department/${department.code}/shared-options" commandName="" cssClass="form-horizontal">

<#-- This is also included by _fields.ftl -->
<#include "_common_fields.ftl" />

<button class="btn btn-primary">Apply options</button>

</@f.form>

</#escape>