<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>


<@f.form method="post" action="${url('/sysadmin/departments/${department.code}/owners/add')}" commandName="addOwner">
<@f.label path="usercode">
<@f.errors path="usercode" cssClass="error" />
Add usercode
</@f.label>
<@f.input path="usercode" cssClass="user-code-picker" />

<input type="submit" value="Add">
</@f.form>
