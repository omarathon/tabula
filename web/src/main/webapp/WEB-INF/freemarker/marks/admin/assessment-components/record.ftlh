<#import "*/modal_macros.ftlh" as modal />

<div class="deptheader">
  <h1>Record marks</h1>
  <h4 class="with-related">for ${assessmentComponent.name} (${assessmentComponent.module.code?upper_case}, ${assessmentComponent.sequence}, ${upstreamAssessmentGroup.occurrence}, ${upstreamAssessmentGroup.academicYear.toString})</h4>
</div>

<p>Use this form to record <strong>unconfirmed actual</strong> component marks for students. If the assessment didn't take place, leave all marks and grades blank.</p>

<#assign submitUrl><@routes.marks.assessmentcomponent_marks assessmentComponent upstreamAssessmentGroup /></#assign>
<#assign generateUrl><@routes.marks.assessmentcomponent_generateGrades assessmentComponent /></#assign>

<div>
  <!-- Nav tabs -->
  <ul class="nav nav-tabs" role="tablist">
    <li role="presentation" class="active"><a href="#upload" aria-controls="upload" role="tab" data-toggle="tab">Upload</a></li>
    <li role="presentation"><a href="#webform" aria-controls="webform" role="tab" data-toggle="tab">Web Form</a></li>
  </ul>
  <!-- Tab panes -->
  <div class="tab-content">
    <div role="tabpanel" class="tab-pane active" id="upload">
      <p>You can upload marks and grades in a spreadsheet, which must be an XLSX file (i.e. created in Microsoft Office 2007 or later). The spreadsheet
        should have the following column headings: <strong>University ID</strong>, <strong>Mark</strong>,
        <strong>Grade</strong> and <strong>Comments</strong>.
        You can use this <a href="<@routes.marks.assessmentcomponent_marks_template assessmentComponent upstreamAssessmentGroup />">generated spreadsheet</a> as a template.</p>

      <p>Enter the results in the following columns (all other columns are locked):</p>

      <ul>
        <li>Column B - Mark</li>
        <li>Column C - Grade</li>
        <li>Column D - Comments</li>
      </ul>

      <p>Note that you can upload any of the following: marks only; marks and grades only; marks and comments only; marks, grades and comments.</p>

      <@f.form method="post" enctype="multipart/form-data" action=submitUrl?markup_string modelAttribute="command">
        <h3>Select file</h3>

        <@bs3form.labelled_form_group path="file" labelText="File">
          <input type="file" name="file.upload" />
        </@bs3form.labelled_form_group>

        <div class="buttons form-group">
          <button type="submit" class="btn btn-primary">Upload</button>
          <a class="btn btn-default dirty-check-ignore" href="<@routes.marks.assessmentcomponents assessmentComponent.module.adminDepartment upstreamAssessmentGroup.academicYear />">Cancel</a>
        </div>
      </@f.form>
    </div>
    <div role="tabpanel" class="tab-pane" id="webform">
      <div class="fix-area">
        <@f.form method="post" action=submitUrl?markup_string modelAttribute="command" cssClass="marks-form">
          <table class="table table-striped">
            <thead>
              <tr>
                <th class="col-sm-1">University ID</th>
                <th class="col-sm-2">Mark</th>
                <th class="col-sm-1">Grade</th>
                <th class="col-sm-2">New mark</th>
                <th class="col-sm-2">New grade</th>
                <th class="col-sm-4">Comments</th>
              </tr>
            </thead>
            <tbody>
              <#list studentMarkRecords as student>
                <tr>
                  <td>${student.universityId}<#if !student.currentMember> <span class="label label-warning tabula-tooltip" data-title="Permanently withdrawn">PWD</span></#if></td>
                  <td>
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

                    <#if student.history?has_content>
                      <button type="button" class="btn btn-default btn-xs" data-toggle="modal" data-target="#mark-history-modal-${student.universityId}"><i class="fal fa-history"></i> View history</button>

                      <@modal.modal id="mark-history-modal-${student.universityId}">
                        <@modal.wrapper cssClass="modal-lg">
                          <@modal.header>
                            <h3 class="modal-title">Mark history for ${student.universityId}</h3>
                          </@modal.header>
                          <@modal.body>
                            <table class="table table-condensed table-striped">
                              <thead>
                                <tr>
                                  <th class="col-sm-3">Date</th>
                                  <th class="col-sm-3">User</th>
                                  <th class="col-sm-1">Mark</th>
                                  <th class="col-sm-1">Grade</th>
                                  <th class="col-sm-4">Comments</th>
                                </tr>
                              </thead>
                              <tbody>
                                <#list student.history as history>
                                  <tr>
                                    <td>
                                      <@fmt.date date=history.updatedDate />
                                    </td>
                                    <td>${history.updatedBy.fullName!(history.updatedBy.userId)}</td>
                                    <td>${history.mark!'-'}</td>
                                    <td>${history.grade!'-'}</td>
                                    <td>${history.comments!}</td>
                                  </tr>
                                </#list>
                              </tbody>
                            </table>
                          </@modal.body>
                          <@modal.footer>
                            <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
                          </@modal.footer>
                        </@modal.wrapper>
                      </@modal.modal>
                    </#if>
                  </td>
                  <td>
                    ${student.grade!}
                  </td>
                  <td>
                    <@bs3form.labelled_form_group path="students[${student.universityId}].mark">
                      <div class="input-group">
                        <@f.input path="students[${student.universityId}].mark" cssClass="form-control" autocomplete="off" type="number" min="0" max="100" />
                        <div class="input-group-addon">%</div>
                      </div>
                    </@bs3form.labelled_form_group>
                  </td>
                  <td>
                    <@bs3form.labelled_form_group path="students[${student.universityId}].grade">
                      <#if isGradeValidation>
                        <@f.input path="students[${student.universityId}].grade" cssClass="form-control auto-grade" data\-mark="students[${student.universityId}].mark" data\-generate\-url=generateUrl?markup_string />
                        <@f.select path="students[${student.universityId}].grade" cssClass="form-control" cssStyle="display: none;" disabled=true />
                      <#else>
                        <@f.input path="students[${student.universityId}].grade" cssClass="form-control" />
                      </#if>
                    </@bs3form.labelled_form_group>
                  </td>
                  <td>
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
    </div>
  </div>
</div>