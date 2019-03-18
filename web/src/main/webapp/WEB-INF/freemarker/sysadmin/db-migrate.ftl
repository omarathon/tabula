<#escape x as x?html>
  <h1>Database migration</h1>

  <div class="alert alert-danger">
    <p>This will copy all data from an external <strong>Oracle</strong> datasource into the currently connected <code>PostgreSQL</code> datasource.</p>

    <p>All existing data will be deleted!!!</p>
  </div>

  <@f.form method="post" action="" modelAttribute="">
    <@bs3form.labelled_form_group labelText="JDBC Url">
      <input type="text" name="jdbcUrl" class="form-control">
    </@bs3form.labelled_form_group>

    <@bs3form.labelled_form_group labelText="Username">
      <input type="text" name="username" class="form-control">
    </@bs3form.labelled_form_group>

    <@bs3form.labelled_form_group labelText="Password">
      <input type="password" name="password" class="form-control">
    </@bs3form.labelled_form_group>

    <@bs3form.form_group>
      <button type="submit" class="btn btn-danger">Delete existing data and migrate database</button>
    </@bs3form.form_group>
  </@f.form>
</#escape>