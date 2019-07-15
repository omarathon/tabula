<#import "*/cm2_macros.ftl" as cm2 />
<#import "*/marking_macros.ftl" as marking_macros />
<#escape x as x?html>

  <@cm2.assignmentHeader "Submit marks and feedback" assignment />
  <div>
    <!-- Nav tabs -->
    <ul class="nav nav-tabs" role="tablist">
      <li role="presentation" class="active"><a href="#upload" aria-controls="upload" role="tab" data-toggle="tab">Upload</a></li>
      <li role="presentation"><a href="#webform" aria-controls="webform" role="tab" data-toggle="tab">Web Form</a></li>
    </ul>
    <!-- Tab panes -->
    <div class="tab-content">
      <div role="tabpanel" class="tab-pane active" id="upload">
        <p>You can upload marks and feedback in a spreadsheet, which must be an XLSX file (i.e. created in Microsoft Office 2007 or later). The spreadsheet
          should have the following column headings: <strong>University ID</strong>, <strong>Mark</strong>,
          <strong>Grade</strong><#list assignment.feedbackFields as field><#if field_has_next>,<#else> and</#if> <strong>${field.label}</strong></#list>. You
          can use this <a href="${templateUrl}">generated spreadsheet</a> as a template.</p>
        <p>Enter the results in the following columns (all other columns are locked):</p>
        <ul>
          <li>Column B - Mark</li>
          <li>Column C - Grade</li>
          <#list assignment.feedbackFields as field>
            <li>Column ${"DEFGHIJKLMNOPQRSTUVXYZ"[field_index]} - ${field.label}</li>
          </#list>
        </ul>
        <p>Note that you can upload any of the following: marks only; marks and grades only; marks, grades and feedback.</p>
        <@f.form method="post" enctype="multipart/form-data" action="${formUrl}" modelAttribute="command">
          <h3>Select file</h3>
          <p id="multifile-column-description">
            <#include "/WEB-INF/freemarker/multiple_upload_help.ftl" />
          </p>
          <@bs3form.labelled_form_group path="file" labelText="Files">
            <input type="file" name="file.upload" multiple />
          </@bs3form.labelled_form_group>

          <input type="hidden" name="isfile" value="true" />

          <div class="buttons form-group">
            <button type="submit" class="btn btn-primary">Upload</button>
            <a class="btn btn-default" href="${cancelUrl}">Cancel</a>
          </div>
        </@f.form>
      </div>
      <div role="tabpanel" class="tab-pane" id="webform">
        <p>Click the add button below to enter marks and feedback for a student.</p>
        <@f.form cssClass="marks-web-form" method="post" action="${formUrl}" modelAttribute="command">
          <div class="row hidden-xs hidden-sm">
            <div class="col-md-2">
              <label>University ID</label>
            </div>
            <#assign feedbackColumnsWidth=7/>
            <#if assignment.showSeatNumbers>
              <#assign feedbackColumnsWidth=6/>
              <div class="col-md-1">
                <label>Seat</label>
              </div>
            </#if>
            <#assign feedbackFieldColumnWidth=(feedbackColumnsWidth / assignment.feedbackFields?size)?floor />
            <div class="col-md-2">
              <label>Mark</label>
            </div>
            <div class="col-md-1">
              <label>Grade</label>
            </div>
            <#list assignment.feedbackFields as field>
              <div class="col-md-${feedbackFieldColumnWidth}">
                <label>${field.label}</label>
              </div>
            </#list>
          </div>
          <#list command.existingMarks as markItem>
            <div class="row mark-row">
              <div class="col-md-2">
                <div class="form-group">
                  <input class="form-control" value="${markItem.id}" name="marks[${markItem_index}].id" type="text" readonly="readonly" />
                </div>
              </div>
              <#if assignment.showSeatNumbers>
                <div class="col-md-1">
                  <div class="form-group">
                    <input class="form-control" value="${assignment.getSeatNumber(markItem.user(assignment))!""}" type="text" readonly="readonly" />
                  </div>
                </div>
              </#if>
              <div class="col-md-2">
                <div class="form-group">
                  <div class="input-group">
                    <input class="form-control" name="marks[${markItem_index}].actualMark" value="<#if markItem.actualMark??>${markItem.actualMark}</#if>"
                           placeholder="Mark" type="number" />
                    <div class="input-group-addon">%</div>
                  </div>
                </div>
              </div>
              <div class="col-md-1">
                <div class="form-group">
                  <#if isGradeValidation>
                    <#assign generateUrl><@routes.cm2.generateGradesForMarks command.assignment /></#assign>
                    <div class="input-group">
                      <input id="auto-grade-${markItem.id}" class="form-control auto-grade" name="marks[${markItem_index}].actualGrade"
                             value="<#if markItem.actualGrade??>${markItem.actualGrade}</#if>" type="text" />
                      <select name="marks[${markItem_index}].actualGrade" class="form-control" disabled style="display:none;"></select>
                    </div>
                    <@marking_macros.autoGradeOnlineScripts "marks[${markItem_index}].actualMark" markItem.id generateUrl />
                  <#else>
                    <div class="form-group">
                      <div class="input-group">
                        <input name="marks[${markItem_index}].actualGrade" class="form-control"
                               value="<#if markItem.actualGrade??>${markItem.actualGrade}</#if>" type="text" placeholder="Grade" />
                      </div>
                    </div class="form-group">
                  </#if>
                </div>
              </div>
              <#list assignment.feedbackFields as field>
                <div class="col-md-${feedbackFieldColumnWidth}">
                  <div class="form-group">
                  <textarea class="small-textarea form-control" name="marks[${markItem_index}].fieldValues[${field.name}]"
                            placeholder="${field.label}"><#if markItem.fieldValues[field.name]??>${markItem.fieldValues[field.name]}</#if></textarea>
                  </div>
                </div>
              </#list>
            </div>
          </#list>
          <input type="hidden" name="isfile" value="false" />
          <div class="buttons form-group">
            <button type="submit" class="btn btn-primary">Save</button>
            <a class="btn btn-default" href="${cancelUrl}">Cancel</a>
          </div>
        </@f.form>
      </div>
    </div>
  </div>
</#escape>