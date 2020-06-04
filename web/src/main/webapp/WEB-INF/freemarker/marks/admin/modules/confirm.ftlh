<div class="deptheader">
  <h1>Confirm module marks</h1>
  <h4 class="with-related">for ${module.code?upper_case}-${cats?string["0.#"]} ${module.name} (${academicYear.toString}, ${occurrence})</h4>
</div>

<#assign submitUrl><@routes.marks.module_marks_confirm module cats academicYear occurrence /></#assign>
<#assign cancelUrl><@routes.marks.adminhome module.adminDepartment academicYear /></#assign>

<#if alreadyConfirmed?has_content>
  <div class="alert alert-success">
      <@fmt.p number=alreadyConfirmed?size singular="student" plural="students" /> marks are already confirmed
  </div>
</#if>
<#if studentsToConfirm?has_content>
  <p>
    The following module results and all component results will be confirmed as actual marks.
    Other departments will be made aware that these marks have been approved by a Pre-examination Module Board.
    Any changes to confirmed actual marks <strong>must</strong> be communicated to any other departments with students taking this module.
  </p>
<#else>
  <div class="alert alert-info">There are no students with unconfirmed actual marks for this module.</div>
</#if>

<div class="fix-area">
    <@f.form method="post" action=submitUrl?markup_string modelAttribute="command">
        <@f.hidden name="confirm" value="true" />
        <#if studentModuleRecords?has_content>
          <table class="table table-striped table-sortable">
            <thead>
            <tr>
              <th class="sortable">SCJ Code</th>
              <th class="sortable">Mark</th>
              <th class="sortable">Grade</th>
              <th class="sortable">Result</th>
              <th>Comment</th>
            </tr>
            </thead>
            <tbody>
            <#list studentsToConfirm as record>
                <#assign student = record._1() />
              <tr>
                <td class="col-md-2">${student.scjCode}</td>
            <td class="col-md-2">
                ${student.mark!'-'}
            </td>
            <td class="col-md-2">
                ${student.grade!'-'}
            </td>
            <td class="col-md-2">
                ${(student.result.dbValue)!'-'}
            </td>
            <td class="col-md-4"><@f.input path="comments[${student.scjCode}]" cssClass="form-control" /></td>
          </tr>
        </#list>
        <#list alreadyConfirmed as record>
          <#assign student = record._1() />
          <tr class="info">
            <td class="col-md-2">${student.scjCode} - already confirmed</td>
            <td class="col-md-2">
                ${student.mark!'-'}
            </td>
            <td class="col-md-2">
              ${student.grade!'-'}
            </td>
            <td class="col-md-2">
              ${(student.result.dbValue)!'-'}
            </td>
            <td class="col-md-4"><@f.input path="comments[${student.scjCode}]" cssClass="form-control" readonly=true /></td>
          </tr>
        </#list>
        </tbody>
      </table>
    </#if>

    <div class="submit-buttons fix-footer">
      <#if studentsToConfirm?has_content><input type="submit" class="btn btn-primary" value="Confirm" /></#if>
      <a class="btn btn-default dirty-check-ignore" href="${cancelUrl?markup_string}">Cancel</a>
    </div>
  </@f.form>
</div>
