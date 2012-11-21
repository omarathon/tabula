<#assign department=command.department />
<#assign markScheme=command.markScheme />
<#assign form_url><@routes.markschemedelete markScheme /></#assign>
<#escape x as x?html>
<#compress>

<p>Are you sure you want to delete <strong>"${markScheme.name}"</strong>?</p>

<@f.form method="post" action="${form_url}" commandName="command" cssClass="form-horizontal">
<@f.errors cssClass="error form-errors" />

<input type="submit" class="btn btn-danger" value="Delete" />

<a class="btn" href="<@routes.markschemelist department />">Cancel</a>

</@f.form>

</#compress>
</#escape>