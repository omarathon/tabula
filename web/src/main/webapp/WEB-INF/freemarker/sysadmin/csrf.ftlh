<h1>CSRF administrating screen page</h1>

<@f.form modelAttribute="command">
    <@bs3form.labelled_form_group "enforce" "CSRF filter mode">
        <@bs3form.radio>
            <@f.radiobutton path="enforce" value="false" />
          Report only 🐶
        </@bs3form.radio>
        <@bs3form.radio>
            <@f.radiobutton path="enforce" value="true" />
          Enforce 🐺
        </@bs3form.radio>
    </@bs3form.labelled_form_group>

  <div class="form-group">
    <button type="submit" class="btn btn-primary">Apply</button>
    <a href="${url('/sysadmin')}" class="btn btn-default">Cancel</a>
  </div>
</@f.form>
