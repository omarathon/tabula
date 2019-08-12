<#escape x as x?html>

  <@f.form method="post" class="double-submit-protection" modelAttribute="command">
    <@bs3form.labelled_form_group path="text" labelText="Descriptor">
      <@f.textarea path="text" cssClass="form-control" rows="5" />
    </@bs3form.labelled_form_group>

    <@bs3form.labelled_form_group path="markPoints" labelText="Mark points">
      <div class="form-text text-muted">
        Select the mark points for which this descriptor applies.
      </div>
      <#list markPoints as markPoint>
        <@bs3form.checkbox>
          <@f.checkbox path="markPoints" value=markPoint label="${markPoint.mark} (${markPoint.name})" />
        </@bs3form.checkbox>
      </#list>
    </@bs3form.labelled_form_group>

    <@bs3form.form_group>
      <button type="submit" class="btn btn-primary">Save</button>
      <a href="<@routes.admin.markingdescriptors department />" class="btn btn-default">Cancel</a>
    </@bs3form.form_group>
  </@f.form>

</#escape>
