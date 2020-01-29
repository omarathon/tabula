<h1>Manage mark upload to SITS</h1>

<div class="fix-area">
  <#assign actionUrl><@routes.cm2.manageMarksClosure /></#assign>
  <@f.form modelAttribute="command" action=actionUrl?markup_string>
    <table class="table table-striped departments-table">
      <thead>
        <tr>
          <th rowspan="2">Code</th>
          <th rowspan="2">Name</th>
          <th rowspan="2">Upload marks to SITS?</th>
          <th colspan="2">Open for ${previousYear.toString}</th>
          <th colspan="2">Open for ${currentYear.toString}</th>
        </tr>
        <tr>
          <th>
            UG<br />
            <input type="checkbox" class="collection-openForPreviousYearUG-all">
          </th>
          <th>
            PG<br />
            <input type="checkbox" class="collection-openForPreviousYearPG-all">
          </th>
          <th>
            UG<br />
            <input type="checkbox" class="collection-openForCurrentYearUG-all">
          </th>
          <th>
            PG<br />
            <input type="checkbox" class="collection-openForCurrentYearPG-all">
          </th>
        </tr>
      </thead>
      <tbody>
        <#list command.departments as department>
          <tr>
            <td>
              <#if department.hasParent>┗</#if>
              ${department.code?upper_case}
            </td>
            <td>
              ${department.name}
              <#if department.hasParent>
                <div class="help-block">Filter: ${department.filterRule.name}</div>
              </#if>
            </td>
            <td><@f.checkbox path="state[${department.code}].uploadCourseworkMarksToSits" /></td>
            <td><@f.checkbox path="state[${department.code}].openForPreviousYearUG" cssClass="collection-openForPreviousYearUG" /></td>
            <td><@f.checkbox path="state[${department.code}].openForPreviousYearPG" cssClass="collection-openForPreviousYearPG" /></td>
            <td><@f.checkbox path="state[${department.code}].openForCurrentYearUG" cssClass="collection-openForCurrentYearUG" /></td>
            <td><@f.checkbox path="state[${department.code}].openForCurrentYearPG" cssClass="collection-openForCurrentYearPG" /></td>
          </tr>
        </#list>
      </tbody>
    </table>

    <div class="submit-buttons fix-footer">
      <button class="btn btn-primary spinnable spinner-auto" type="submit" name="submit" data-loading-text="Saving&hellip;">
        Save
      </button>
      <a class="btn btn-default" href="<@routes.cm2.home />">Cancel</a>
    </div>
  </@f.form>
</div>

<script type="text/javascript" nonce="${nonce()}">
  jQuery(function ($) {
    $('.departments-table')
      .bigList({ checkboxClass: 'collection-openForPreviousYearUG', checkboxAllClass: 'collection-openForPreviousYearUG-all' })
      .bigList({ checkboxClass: 'collection-openForPreviousYearPG', checkboxAllClass: 'collection-openForPreviousYearPG-all' })
      .bigList({ checkboxClass: 'collection-openForCurrentYearUG', checkboxAllClass: 'collection-openForCurrentYearUG-all' })
      .bigList({ checkboxClass: 'collection-openForCurrentYearPG', checkboxAllClass: 'collection-openForCurrentYearPG-all' });
  });
</script>
