<h1>Missed assessments</h1>

<@f.form method="get" action="" modelAttribute="command" cssClass="form-inline" id="report-form">
<@bs3form.labelled_form_group path="startDate" labelText="Start date">
    <@f.input id="startDate" path="startDate" cssClass="date-picker input-small form-control" />
</@bs3form.labelled_form_group>
<@bs3form.labelled_form_group path="endDate" labelText="End date">
    <@f.input id="endDate" path="endDate" cssClass="date-picker input-small form-control" />
</@bs3form.labelled_form_group>

<div class="form-group">
  <button type="submit" class="btn btn-default">Submit</button>
</div>

<#if !errors>
<p></p>

<div class="alert alert-info">
  <p>Report complete</p>
  <div class="btn-toolbar">
    <a href="#" class="btn btn-default" data-loading-text="Building table, please wait&hellip;" id="show-report">Show</a>
    <div class="btn-group">
      <a href="#" class="btn btn-default dropdown-toggle" data-toggle="dropdown">Download&hellip;<span class="caret"></span></a>
      <ul class="dropdown-menu">
        <li><button type="submit" name="format" value="csv">CSV</button></li>
        <li><button type="submit" name="format" value="xlsx">Excel</button></li>
        <li><button type="submit" name="format" value="xml">XML</button></li>
      </ul>
    </div>
  </div>
</div>
</#if>
</@f.form>

<div id="report-result"></div>

