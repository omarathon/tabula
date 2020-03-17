Check how many TCA submissions are in-progress

<@f.form method="post" action="${url('/sysadmin/tca/check-in-progress')}" modelAttribute="checkTCAInProgressCommand" >
    <@bs3form.labelled_form_group path="since" labelText="Since date">
        <@f.input path="since" cssClass="form-control date-picker" autocomplete="off" />
    </@bs3form.labelled_form_group>

    <@bs3form.labelled_form_group>
      <input type="submit" value="Check" class="btn btn-primary">
      <a class="btn btn-default" href="<@url page="/sysadmin" />">Cancel</a>
    </@bs3form.labelled_form_group>
</@f.form>

<#if result??>
  Total number of in-progress TCA submissions sent since ${checkTCAInProgressCommand.since}: ${result?size}

  <@f.form method="post" action="${url('/sysadmin/tca/prod-in-progress')}" modelAttribute="checkTCAInProgressCommand">
    <div class="input-group">
        <@f.input type="hidden" path="since" cssClass="form-control date-picker" autocomplete="off" />
      <input class="btn btn-danger" type="submit" value="Prod these in-progress TCA submissions" /></span>
    </div>
  </@f.form>

</#if>

