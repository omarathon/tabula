<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#import "../cm2_macros.ftl" as cm2_macros />
<#escape x as x?html>
<#--
HFC-166 Don't use #compress on this file because
the comments textarea needs to maintain newlines.
-->
<#assign submitUrl><@routes.cm2.enrolment assignment /></#assign>
<@f.form method="post" action="${submitUrl}" cssClass="form-horizontal">
	<@f.errors cssClass="error form-errors" />
	<#import "../assignment_membership_picker_macros.ftl" as membership_picker />
	<@membership_picker.header command />
	<@membership_picker.fieldset command submitUrl/>
</@f.form>
</#escape>