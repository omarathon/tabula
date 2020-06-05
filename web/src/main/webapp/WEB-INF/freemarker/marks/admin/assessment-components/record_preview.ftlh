<div class="deptheader">
  <h1>Record marks</h1>
  <h4 class="with-related">for ${assessmentComponent.name} (${assessmentComponent.module.code?upper_case}, ${assessmentComponent.sequence}, ${upstreamAssessmentGroup.occurrence}, ${upstreamAssessmentGroup.academicYear.toString})</h4>
</div>

<#assign submitUrl><@routes.marks.assessmentcomponent_marks assessmentComponent upstreamAssessmentGroup /></#assign>

<#if changes?size == 0>
  <p>There were no changes to marks or grades.</p>

  <p><a href="<@routes.marks.adminhome assessmentComponent.module.adminDepartment upstreamAssessmentGroup.academicYear />">Back to assessment components</a></p>
<#else>
  <p>The following marks and grades will be changed:</p>

  <div class="fix-area">
    <@f.form method="post" action=submitUrl?markup_string modelAttribute="command">
      <@f.hidden name="confirm" value="true" />

      <table class="table table-striped">
        <thead>
          <tr>
            <th>University ID</th>
            <th>Mark</th>
            <th>Grade</th>
            <th>Comment</th>
          </tr>
        </thead>
        <tbody>
          <#list changes as change>
            <#assign student = change._1() />
            <#assign c = change._2() />

            <tr>
              <td class="col-md-2">${student.universityId}<#if !student.currentMember> <span class="very-subtle">(not current)</span></#if></td>
              <td class="col-md-2">
                <@f.hidden path="students[${student.universityId}].mark" />
                ${student.mark!'-'}
                <#if "${student.mark!}" != c.mark!>
                  <i class="fal fa-arrow-right"></i>
                  ${c.mark!}
                </#if>
              </td>
              <td class="col-md-2">
                <@f.hidden path="students[${student.universityId}].grade" />
                ${student.grade!'-'}
                <#if student.grade! != c.grade!>
                  <i class="fal fa-arrow-right"></i>
                  ${c.grade!}
                </#if>
              </td>
              <td class="col-md-4"><@f.input path="students[${student.universityId}].comments" cssClass="form-control" readonly=true /></td>
            </tr>
          </#list>
        </tbody>
      </table>

      <div class="submit-buttons fix-footer">
        <input type="submit" class="btn btn-primary" name="action" value="Confirm" />
        <button type="submit" class="btn btn-default dirty-check-ignore" name="action" value="Cancel">Back</button>
      </div>
    </@f.form>
  </div>
</#if>
