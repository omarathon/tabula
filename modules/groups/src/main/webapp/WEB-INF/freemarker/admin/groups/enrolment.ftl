<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>

<#assign command=editGroupSetEnrolmentCommand />

<#assign submitUrl><@routes.enrolment module /></#assign>
<@f.form method="post" action="${submitUrl}" commandName="editGroupSetEnrolmentCommand" cssClass="form-horizontal">

<@f.errors cssClass="error form-errors" />

<#include "groups_membership_picker.ftl" />

</@f.form>
</#escape>