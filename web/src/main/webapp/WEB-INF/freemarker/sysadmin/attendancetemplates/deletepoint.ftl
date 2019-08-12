<#escape x as x?html>

  <h1>Delete point</h1>

  <#assign action><@url page="/sysadmin/attendancetemplates/${point.scheme.id}/points/${point.id}/delete"/></#assign>
  <@f.form id="deleteMonitoringPointSet" action=action method="POST"
        class="form-inline">

    <p>You are deleting point: ${point.name}. Are you sure?</p>

    <input type="submit" value="Delete" class="btn btn-danger" />
    <a class="btn btn-default" href="<@url page="/sysadmin/attendancetemplates/${point.scheme.id}/edit"/>">Cancel</a>

  </@f.form>

</#escape>