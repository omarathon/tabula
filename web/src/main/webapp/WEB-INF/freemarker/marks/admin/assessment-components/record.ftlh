<div class="deptheader">
  <h1>Record marks</h1>
  <h4 class="with-related">for ${assessmentComponent.name} (${assessmentComponent.module.code?upper_case}, ${upstreamAssessmentGroup.academicYear.toString})</h4>
</div>

<div class="fix-area">
  <#assign submitUrl><@routes.marks.assessmentcomponent_marks assessmentComponent upstreamAssessmentGroup /></#assign>
  <#assign generateUrl><@routes.marks.assessmentcomponent_generateGrades assessmentComponent /></#assign>

  <@f.form method="post" action=submitUrl?markup_string modelAttribute="command">
    <table class="table table-striped">
      <thead>
        <tr>
          <th>University ID</th>
          <th>Mark</th>
          <th>Grade</th>
          <th>New mark</th>
          <th>New grade</th>
          <th>Comments</th>
        </tr>
      </thead>
      <tbody>
        <#list studentMarkRecords as student>
          <tr>
            <td class="col-md-2">${student.universityId}<#if !student.currentMember> <span class="very-subtle">(not current)</span></#if></td>
            <td class="col-md-1">
              ${student.mark!}
              <#if student.needsWritingToSits>
                <span class="tabula-tooltip" data-title="Waiting to be written to SITS">
                  <i class="fa-fw fad fa-cloud-upload" aria-hidden="true"></i>
                  <span class="sr-only">Waiting to be written to SITS</span>
                </span>
              <#elseif student.outOfSync>
                <span class="tabula-tooltip" data-title="Mark is out of sync with current mark in SITS">
                  <i class="fa-fw fad fa-exclamation-circle" aria-hidden="true"></i>
                  <span class="sr-only">Mark is out of sync with current mark in SITS</span>
                </span>
              <#elseif student.agreed>
                <span class="tabula-tooltip" data-title="Agreed mark published to student">
                  <i class="fa-fw fad fa-check-circle" aria-hidden="true"></i>
                  <span class="sr-only">Agreed mark published to student</span>
                </span>
              </#if>
            </td>
            <td class="col-md-1">
              ${student.grade!}
            </td>
            <td class="col-md-2">
              <@bs3form.labelled_form_group path="students[${student.universityId}].mark">
                <div class="input-group">
                  <@f.input path="students[${student.universityId}].mark" cssClass="form-control" autocomplete="off" type="number" min="0" max="100" />
                  <div class="input-group-addon">%</div>
                </div>
              </@bs3form.labelled_form_group>
            </td>
            <td class="col-md-2">
              <@bs3form.labelled_form_group path="students[${student.universityId}].grade">
                <#if isGradeValidation>
                  <@f.input path="students[${student.universityId}].grade" cssClass="form-control auto-grade" data\-mark="students[${student.universityId}].mark" data\-generate\-url=generateUrl?markup_string />
                  <@f.select path="students[${student.universityId}].grade" cssClass="form-control" cssStyle="display: none;" disabled=true />
                <#else>
                  <@f.input path="students[${student.universityId}].grade" cssClass="form-control" />
                </#if>
              </@bs3form.labelled_form_group>
            </td>
            <td class="col-md-4">
              <@bs3form.labelled_form_group path="students[${student.universityId}].comments">
                <@f.input path="students[${student.universityId}].comments" cssClass="form-control" />
              </@bs3form.labelled_form_group>
            </td>
          </tr>
        </#list>
      </tbody>
    </table>

    <div class="submit-buttons fix-footer">
      <input type="submit" class="btn btn-primary" value="Save" />
      <a class="btn btn-default dirty-check-ignore" href="<@routes.marks.assessmentcomponents assessmentComponent.module.adminDepartment upstreamAssessmentGroup.academicYear />">Cancel</a>
    </div>
  </@f.form>
</div>
