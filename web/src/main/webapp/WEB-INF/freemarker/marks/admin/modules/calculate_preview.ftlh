<div class="deptheader">
  <h1>Calculate module marks</h1>
  <h4 class="with-related">for ${module.code?upper_case}-${cats?string["0.#"]} ${module.name} (${academicYear.toString}, ${occurrence})</h4>
</div>

<#assign submitUrl><@routes.marks.module_marks module cats academicYear occurrence /></#assign>

<#if changes?size == 0>
  <p>There were no changes to marks, grades or results.</p>

  <p><a href="<@routes.marks.adminhome module.adminDepartment academicYear />">Back to module marks</a></p>
<#else>
  <p>The following marks, grades and results will be changed:</p>

  <div class="fix-area">
    <@f.form method="post" action=submitUrl?markup_string modelAttribute="command">
      <@f.hidden name="confirm" value="true" />

      <table class="table table-striped">
        <thead>
          <tr>
            <th>SCJ Code</th>
            <th>Mark</th>
            <th>Grade</th>
            <th>Result</th>
            <th>Comment</th>
          </tr>
        </thead>
        <tbody>
          <#list changes as change>
            <#assign student = change._1() />
            <#assign c = change._4() />

            <tr>
              <td class="col-md-2">${student.scjCode}</td>
              <td class="col-md-2">
                <@f.hidden path="students[${student.scjCode}].mark" />
                ${student.mark!'-'}
                <#if "${student.mark!}" != c.mark!>
                  <i class="fal fa-arrow-right"></i>
                  ${c.mark!}
                </#if>
              </td>
              <td class="col-md-2">
                <@f.hidden path="students[${student.scjCode}].grade" />
                ${student.grade!'-'}
                <#if student.grade! != c.grade!>
                  <i class="fal fa-arrow-right"></i>
                  ${c.grade!}
                </#if>
              </td>
              <td class="col-md-2">
                <@f.hidden path="students[${student.scjCode}].result" />
                ${(student.result.dbValue)!'-'}
                <#if (student.result.dbValue)! != c.result!>
                  <i class="fal fa-arrow-right"></i>
                  ${c.result!}
                </#if>
              </td>
              <td class="col-md-4"><@f.input path="students[${student.scjCode}].comments" cssClass="form-control" readonly=true /></td>
            </tr>
          </#list>
        </tbody>
      </table>

      <div class="submit-buttons fix-footer">
        <input type="submit" class="btn btn-primary" value="Confirm" />
        <a class="btn btn-default dirty-check-ignore" href="${returnTo}">Cancel</a>
      </div>
    </@f.form>
  </div>
</#if>
