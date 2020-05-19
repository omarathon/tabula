<h1>Assessment component deadlines</h1>

<p>I am a monster who eats spreadsheets, feed me</p>

<#if (errors.hasErrors())!false>
  <div class="alert alert-danger">
    <ul class="list-unstyled">
      <#list errors.fieldErrors as error>
        <li>${error.field}: <@spring.message code=error.code arguments=error.arguments/></li>
      </#list>
    </ul>
  </div>
</#if>

<#assign action><@url page="/sysadmin/departments/${department.code}/assessment-component-deadlines"/></#assign>
<@f.form method="post" modelAttribute="command" action=action?markup_string enctype="multipart/form-data">
  <@bs3form.labelled_form_group path="file" labelText="File">
    <input type="file" name="file.upload" />
  </@bs3form.labelled_form_group>

  <div class="buttons form-group">
    <button type="submit" class="btn btn-primary"><i class="fad fa-pastafarianism"></i> FEED ME</button>
  </div>
</@f.form>
