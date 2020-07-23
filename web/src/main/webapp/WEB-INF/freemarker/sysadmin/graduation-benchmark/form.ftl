<#escape x as x?html>
  <h1>Graduation benchmark</h1>

  <@f.form modelAttribute="command" action="${url('/sysadmin/graduation-benchmark')}" method="POST" cssClass="form">
    <@bs3form.labelled_form_group "ids" "University IDs or SPR/SCJ codes (one per line)">
      <@f.textarea path="ids" cssClass="form-control text big-textarea" />
    </@bs3form.labelled_form_group>

    <div class="submit-buttons">
      <input class="btn btn-primary" type="submit" value="Generate">
    </div>
  </@f.form>
</#escape>
