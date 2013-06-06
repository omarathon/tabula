<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>
<#-- 
HFC-166 Don't use #compress on this file because
the comments textarea needs to maintain newlines. 
-->
<#assign command=editAssignmentEnrolmentCommand />

<@f.form method="post" action="<@routes.assignmentenrolment module />" commandName="editAssignmentEnrolmentCommand" cssClass="form-horizontal">

<@f.errors cssClass="error form-errors" />

<#include "assignment_membership_picker.ftl" />

</@f.form>
</#escape>