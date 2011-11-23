<#assign fmt=JspTaglibs["/WEB-INF/tld/fmt.tld"]>
<#assign warwick=JspTaglibs["/WEB-INF/tld/warwick.tld"]>
<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>
<h1>${assignment.name}</h1>

<#if assignment.active> <#-- TODO and not already submitted! -->
	<@f.form method="post" action="/module/${module.code}/${assignment.id}" commandName="submitAssignment">
	<@f.errors cssClass="error form-errors">
	</@f.errors>
	<p>Student: ${user.apparentUser.warwickId}</p>
	
	<p>Form fields here.</p>
	
	<input type="submit" value="Submit">
	</@f.form>
<#else>
	
</#if>

</#escape>