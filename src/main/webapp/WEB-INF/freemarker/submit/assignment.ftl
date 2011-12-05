<#assign fmt=JspTaglibs["/WEB-INF/tld/fmt.tld"]>
<#assign warwick=JspTaglibs["/WEB-INF/tld/warwick.tld"]>
<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>
<h1>${assignment.name}</h1>

<#if assignment.active> <#-- TODO and not already submitted! -->

	<p>Submission closes <@warwick.formatDate value=assignment.closeDate pattern="d MMMM yyyy HH:mm:ss (z)" /></p>

	<@f.form cssClass="submission-form" method="post" action="/module/${module.code}/${assignment.id}" commandName="submitAssignment">
	<@f.errors cssClass="error form-errors">
	</@f.errors>
	<p>Student: ${user.apparentUser.warwickId}</p>
	
	<div class="submission-fields">
	
	<#list assignment.fields as field>
	<div class="submission-field">
	<#include "/WEB-INF/freemarker/submit/formfields/${field.template}.ftl" >
	</div>
	</#list>
	
	</div>
	
	<div class="submit-buttons">
	<input type="submit" value="Submit">
	</div>
	</@f.form>
<#else>
	
	Assignment closed <@warwick.formatDate value=assignment.closeDate pattern="d MMMM yyyy HH:mm:ss (z)" />.
	
</#if>

</#escape>