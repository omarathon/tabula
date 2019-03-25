<#escape x as x?html>
  <h1>Object storage</h1>

  <@f.form modelAttribute="command" action="${url('/sysadmin/objectstorage')}" method="POST" cssClass="form">
    <@bs3form.labelled_form_group "ids" "Attachment IDs (one per line)">
      <@f.textarea path="ids" cssClass="form-control text big-textarea" />
    </@bs3form.labelled_form_group>

    <div class="submit-buttons">
      <input class="btn btn-primary" type="submit" value="Search">
    </div>
  </@f.form>
</#escape>