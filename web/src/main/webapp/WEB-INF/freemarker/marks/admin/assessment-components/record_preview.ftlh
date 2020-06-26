<#import "*/modal_macros.ftlh" as modal />
<#import "*/_profile_link.ftl" as pl />
<#import "*/marks_components.ftlh" as components />

<div class="deptheader">
  <h1>Record marks</h1>
  <h4 class="with-related">for ${assessmentComponent.name} (${assessmentComponent.module.code?upper_case}, ${assessmentComponent.sequence}, ${upstreamAssessmentGroup.occurrence}, ${upstreamAssessmentGroup.academicYear.toString})</h4>
</div>

<#assign submitUrl><@routes.marks.assessmentcomponent_marks assessmentComponent upstreamAssessmentGroup /></#assign>

<@modal.modal id="profile-modal" cssClass="profile-subset"></@modal.modal>
<#if changes?size == 0>
  <p>There were no changes to marks or grades.</p>

  <p><a href="<@routes.marks.adminhome assessmentComponent.module.adminDepartment upstreamAssessmentGroup.academicYear />">Back to assessment components</a></p>
<#else>
  <p>The following marks and grades will be changed:</p>

  <div class="fix-area">
    <@f.form method="post" action=submitUrl?markup_string modelAttribute="command">
      <@f.hidden name="confirm" value="true" />

      <table class="table table-striped table-sortable">
        <thead>
          <tr>
            <th class="col-sm-1 sortable">University ID</th>
            <th class="col-sm-2 sortable">First name</th>
            <th class="col-sm-2 sortable">Last name</th>
            <th class="col-sm-2 sortable">Mark</th>
            <th class="col-sm-2 sortable">Grade</th>
            <th class="col-sm-4">Comment</th>
          </tr>
        </thead>
        <tbody>
          <#list changes as change>
            <#assign student = change._1() />
            <#assign c = change._2() />
            <#assign member = membersByUniversityId[student.universityId] />

            <tr>
              <td data-sortby="u${student.universityId}">
                <@f.hidden path="students[${student.universityId}_${student.resitSequence!}].resitSequence" />
                <@pl.profile_link member.universityId />
                ${student.universityId}
                <@components.componentMarkStudentStatus student />
              </td>
              <td>${member.firstName}</td>
              <td>${member.lastName}</td>
              <td data-sortby="${c.mark!-1}">
                <@f.hidden path="students[${student.universityId}_${student.resitSequence!}].mark" />
                ${student.mark!'-'}
                <#if "${student.mark!}" != c.mark!>
                  <i class="fal fa-arrow-right"></i>
                  ${c.mark!}
                </#if>
              </td>
              <td data-sortby="g${c.grade!}">
                <@f.hidden path="students[${student.universityId}_${student.resitSequence!}].grade" />
                <@components.format_grade student.grade!'-' />
                <#if student.grade! != c.grade!>
                  <i class="fal fa-arrow-right"></i>
                  <@components.format_grade c.grade! />
                </#if>
              </td>
              <td><@f.input path="students[${student.universityId}_${student.resitSequence!}].comments" cssClass="form-control" readonly=true tabindex="-1" /></td>
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
