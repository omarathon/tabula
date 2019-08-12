<#escape x as x?html>

  <h1>Delete template</h1>

  <#assign deleteTemplateAction><@url page="/sysadmin/attendancetemplates/${template.id}/delete"/></#assign>
  <@f.form id="deleteMonitoringPointSet" action=deleteTemplateAction method="POST" cssClass="form-inline" modelAttribute="command">

    <p>You are deleting template: ${template.templateName}.</p>

    <p>
      <@bs3form.form_group>
        <@bs3form.checkbox path="confirm">
          <@f.checkbox path="confirm" /> I confirm that I want to delete this template and all its points.
        </@bs3form.checkbox>
      </@bs3form.form_group>
    </p>

    <input type="submit" value="Delete" class="btn btn-danger" />
    <a class="btn btn-default" href="<@url page="/sysadmin/attendancetemplates"/>">Cancel</a>

  </@f.form>

</#escape>