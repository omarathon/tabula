<#-- @ftlvariable name="from_origin" type="java.lang.String" -->
<#import "*/modal_macros.ftlh" as modal />

<div class="deptheader">
  <h1>Calculate module marks</h1>
  <h4 class="with-related">for ${module.code?upper_case}-${cats?string["0.#"]} ${module.name} (${academicYear.toString}, ${occurrence})</h4>
</div>

<p>Use this form to calculate and record <strong>unconfirmed actual</strong> module marks for students.</p>

<#assign submitUrl><@routes.marks.module_marks module cats academicYear occurrence /></#assign>

<div>
  <!-- Nav tabs -->
  <ul class="nav nav-tabs" role="tablist">
    <li role="presentation" <#if (from_origin!"webform") == "webform">class="active"</#if>><a href="#webform" aria-controls="webform" role="tab" data-toggle="tab">Web Form</a></li>
    <li role="presentation" <#if (from_origin!"webform") == "upload">class="active"</#if>><a href="#upload" aria-controls="upload" role="tab" data-toggle="tab">Upload</a></li>
  </ul>
  <!-- Tab panes -->
  <div class="tab-content">
    <div role="tabpanel" class="tab-pane <#if (from_origin!"webform") == "webform">active</#if>" id="webform">
      <div class="fix-area">
        <@f.form method="post" action=submitUrl?markup_string modelAttribute="command" cssClass="marks-form">
          <div class="has-error"><@f.errors path="" cssClass="help-block" /></div>
          <table class="table table-striped">
            <colgroup class="student">
              <col class="student-info" />
            </colgroup>
            <#list assessmentComponents as ac>
              <colgroup class="assessment-component">
                <col class="assessment-component-mark" />
                <col class="assessment-component-grade" />
              </colgroup>
            </#list>
            <colgroup class="module-mark-calculation">
              <col class="calculated-mark" />
              <col class="calculated-grade" />
              <col class="calculated-result" />
            </colgroup>
            <colgroup class="form-fields">
              <col class="form-mark" />
              <col class="form-grade" />
              <col class="form-result" />
              <col class="form-comments" />
            </colgroup>

            <thead>
              <tr>
                <th class="col-sm-1" rowspan="2">SCJ Code</th>

                <#list assessmentComponents as ac>
                  <th colspan="2">${ac.name} (${ac.sequence}, ${(ac.scaledWeighting!0)?string["0.#"]}%)</th>
                </#list>

                <th colspan="3">Overall</th>

                <th class="col-sm-2" rowspan="2">Mark</th>
                <th class="col-sm-1" rowspan="2">Grade</th>
                <th class="col-sm-1" rowspan="2">Result</th>
                <th class="col-sm-2" rowspan="2">Comments</th>
              </tr>

              <tr>
                <#list assessmentComponents as ac>
                  <th>Mark</th>
                  <th>Grade</th>
                </#list>

                <th>Mark</th>
                <th>Grade</th>
                <th>Result</th>
              </tr>
            </thead>
            <tbody>
              <#list studentModuleMarkRecords as studentMarkRecord>
                <#assign student = studentMarkRecord._1() />
                <#assign componentMarks = studentMarkRecord._2() />
                <#assign calculation = studentMarkRecord._3() />

                <tr>
                  <td>
                    ${student.scjCode}

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
                      <button type="button" class="btn btn-default btn-xs tabula-tooltip" data-title="View history" data-toggle="modal" data-target="#mark-history-modal-${student.scjCode}"><i class="fal fa-history" aria-hidden="true"></i><span class="sr-only">View history</span></button>

                      <@modal.modal id="mark-history-modal-${student.scjCode}">
                        <@modal.wrapper cssClass="modal-lg">
                          <@modal.header>
                            <h3 class="modal-title">Mark history for ${student.scjCode}</h3>
                          </@modal.header>
                          <@modal.body>
                            <table class="table table-condensed table-striped">
                              <thead>
                                <tr>
                                  <th class="col-sm-3">Date</th>
                                  <th class="col-sm-3">User</th>
                                  <th class="col-sm-1">Mark</th>
                                  <th class="col-sm-1">Grade</th>
                                  <th class="col-sm-1">Result</th>
                                  <th class="col-sm-3">Comments</th>
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
                                    <td>${(history.result.description)!'-'}</td>
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

                  <#list assessmentComponents as ac>
                    <#assign marks = mapGet(componentMarks, ac) />
                    <#if marks??>
                      <td>
                        ${marks.mark!'-'}

                        <#if marks.needsWritingToSits>
                          <span class="tabula-tooltip" data-title="Waiting to be written to SITS">
                            <i class="fa-fw fad fa-cloud-upload" aria-hidden="true"></i>
                            <span class="sr-only">Waiting to be written to SITS</span>
                          </span>
                        <#elseif marks.outOfSync>
                          <span class="tabula-tooltip" data-title="Mark is out of sync with current mark in SITS">
                            <i class="fa-fw fad fa-exclamation-circle" aria-hidden="true"></i>
                            <span class="sr-only">Mark is out of sync with current mark in SITS</span>
                          </span>
                        <#elseif marks.agreed>
                          <span class="tabula-tooltip" data-title="Agreed mark published to student">
                            <i class="fa-fw fad fa-check-circle" aria-hidden="true"></i>
                            <span class="sr-only">Agreed mark published to student</span>
                          </span>
                        </#if>

                        <#if marks.history?has_content>
                          <button type="button" class="btn btn-default btn-xs tabula-tooltip" data-title="View history" data-toggle="modal" data-target="#mark-history-modal-${marks.universityId}-${ac.sequence}"><i class="fal fa-history" aria-hidden="true"></i><span class="sr-only">View history</span></button>

                          <@modal.modal id="mark-history-modal-${marks.universityId}-${ac.sequence}">
                            <@modal.wrapper cssClass="modal-lg">
                              <@modal.header>
                                <h3 class="modal-title">Mark history for ${marks.universityId}, ${ac.name}</h3>
                              </@modal.header>
                              <@modal.body>
                                <table class="table table-condensed table-striped">
                                  <thead>
                                    <tr>
                                      <th class="col-sm-3">Date</th>
                                      <th class="col-sm-3">User</th>
                                      <th class="col-sm-1">Mark</th>
                                      <th class="col-sm-1">Grade</th>
                                      <th class="col-sm-1">Result</th>
                                      <th class="col-sm-3">Comments</th>
                                    </tr>
                                  </thead>
                                  <tbody>
                                    <#list marks.history as history>
                                      <tr>
                                        <td>
                                          <@fmt.date date=history.updatedDate />
                                        </td>
                                        <td>${history.updatedBy.fullName!(history.updatedBy.userId)}</td>
                                        <td>${history.mark!'-'}</td>
                                        <td>${history.grade!'-'}</td>
                                        <td>${(history.result.description)!'-'}</td>
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
                      <td>${marks.grade!'-'}</td>
                    <#else>
                      <td colspan="2"><span class="very-subtle">N/A</span></td>
                    </#if>
                  </#list>

                  <#if calculation.successful>
                    <td>
                      <#if (student.mark!-1) != (calculation.mark!-1)>
                        ${student.mark!'-'}
                        <i class="fal fa-arrow-right"></i>
                      </#if>
                      ${calculation.mark!'-'}
                    </td>
                    <td>
                      <#if student.grade! != calculation.grade!>
                        ${student.grade!'-'}
                        <i class="fal fa-arrow-right"></i>
                      </#if>
                      ${calculation.grade!'-'}
                    </td>
                    <td>
                      <#if (student.result.description)! != (calculation.result.description)!>
                        ${(student.result.description)!'-'}
                        <i class="fal fa-arrow-right"></i>
                      </#if>
                      ${(calculation.result.description)!'-'}
                    </td>
                  <#else>
                    <td colspan="3">
                      <span class="tabula-tooltip" data-title="${calculation.message}">
                        <i class="fa-fw fad fa-exclamation-triangle" aria-hidden="true"></i>
                        <span class="sr-only">${calculation.message}</span>
                      </span>
                    </td>
                  </#if>

                  <td>
                    <@bs3form.labelled_form_group path="students[${student.scjCode}].mark">
                      <div class="input-group">
                        <@f.input path="students[${student.scjCode}].mark" cssClass="form-control mark-box" autocomplete="off" type="number" min="0" max="100" />
                        <div class="input-group-addon">%</div>
                      </div>
                    </@bs3form.labelled_form_group>
                  </td>
                  <td>
                    <@bs3form.labelled_form_group path="students[${student.scjCode}].grade">
                      <#if isGradeValidation>
                        <#assign generateUrl><@routes.marks.module_generateGrades module cats academicYear occurrence student.scjCode /></#assign>

                        <@f.input path="students[${student.scjCode}].grade" cssClass="form-control auto-grade grade-box" data\-mark="students[${student.scjCode}].mark" data\-generate\-url=generateUrl?markup_string data\-resit=false /> <#-- TODO Resit module regs? -->
                        <@f.select path="students[${student.scjCode}].grade" cssClass="form-control" cssStyle="display: none;" disabled=true />
                      <#else>
                        <@f.input path="students[${student.scjCode}].grade" cssClass="form-control grade-box" />
                      </#if>
                    </@bs3form.labelled_form_group>
                  </td>
                  <td>
                    <@bs3form.labelled_form_group path="students[${student.scjCode}].result">
                      <@f.select path="students[${student.scjCode}].result" cssClass="form-control">
                        <@f.option value="" />
                        <#list moduleResults as result>
                          <@f.option value=result.entryName label=result.description />
                        </#list>
                      </@f.select>
                    </@bs3form.labelled_form_group>
                  </td>
                  <td>
                    <@bs3form.labelled_form_group path="students[${student.scjCode}].comments">
                      <@f.input path="students[${student.scjCode}].comments" cssClass="form-control" />
                    </@bs3form.labelled_form_group>
                  </td>
                </tr>
              </#list>
            </tbody>
          </table>

          <div class="submit-buttons fix-footer">
            <input type="submit" class="btn btn-primary" value="Save" />
            <a class="btn btn-default dirty-check-ignore" href="<@routes.marks.adminhome module.adminDepartment academicYear />">Cancel</a>
          </div>
        </@f.form>
      </div>
    </div>
    <div role="tabpanel" class="tab-pane <#if (from_origin!"webform") == "upload">active</#if>" id="upload">
      <p>You can upload marks and grades in a spreadsheet, which must be an XLSX file (i.e. created in Microsoft Office 2007 or later). The spreadsheet
        should have the following column headings: <strong>SCJ Code</strong>, <strong>Mark</strong>,
        <strong>Grade</strong>, <strong>Result</strong> and <strong>Comments</strong>.
        You can use this <a href="<@routes.marks.module_marks_template module cats academicYear occurrence />">generated spreadsheet</a> as a template.
      </p>

      <p>Enter the corresponding results for each student in the following columns:</p>

      <ul>
        <li>Column B - Mark</li>
        <li>Column C - Grade</li>
        <li>Column D - Result</li>
        <li>Column E - Comments</li>
      </ul>

      <@f.form method="post" enctype="multipart/form-data" action=submitUrl?markup_string modelAttribute="command">
        <h3>Select file</h3>

        <@bs3form.labelled_form_group path="file" labelText="File">
          <input type="file" name="file.upload" />
        </@bs3form.labelled_form_group>

        <div class="buttons form-group">
          <button type="submit" class="btn btn-primary">Upload</button>
          <a class="btn btn-default dirty-check-ignore" href="<@routes.marks.adminhome module.adminDepartment academicYear />">Cancel</a>
        </div>
      </@f.form>
    </div>
  </div>
</div>
