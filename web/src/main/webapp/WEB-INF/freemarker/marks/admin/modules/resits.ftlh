<#import "*/modal_macros.ftlh" as modal />
<#import "*/_profile_link.ftl" as pl />

<div class="deptheader">
  <h1>Generate resits</h1>
  <h4 class="with-related">for ${sitsModuleCode} ${module.name} (${academicYear.toString}, ${occurrence})</h4>
</div>

<#macro detail label>
  <div class="row form-horizontal">
    <div class="col-sm-6">
      ${label}
    </div>
    <div class="col-sm-6">
      <#nested>
    </div>
  </div>
</#macro>

<#assign hasErrors = false />
<@spring.hasBindErrors name="command">
  <#assign hasErrors = errors.hasGlobalErrors() />
  <#if errors.hasErrors()>
    <div class="alert alert-danger">
      <#if errors.hasGlobalErrors()>
        <#list errors.globalErrors as e>
          <div><@spring.message message=e /></div>
        </#list>
      <#else>
        <div>See the errors below.</div>
      </#if>
    </div>
  </#if>
</@spring.hasBindErrors>

<#assign submitUrl><@routes.marks.module_marks_resits sitsModuleCode academicYear occurrence /></#assign>
<#assign cancelUrl><@routes.marks.adminhome module.adminDepartment academicYear /></#assign>

<@modal.modal id="profile-modal" cssClass="profile-subset"></@modal.modal>

<@f.form method="post" action=submitUrl?markup_string modelAttribute="command">
    <@f.hidden name="confirm" value="true" />

    <#if requiresResits?has_content>
        <p>
          <@fmt.p number=requiresResits?size singular="student" plural="students" /> have received a grade which indicates that they have further assessment requirements.
          Please indicate for each whether this is an "Examination" (i.e. an online test to be administered via the Alternative Exams Portal (AEP)) or
          a written "Assignment", by selecting the appropriate type from the drop-down.
        </p>

        <p>
          You can choose the assessment type to use for each assessment component below. You can also specify this for each student.
        </p>

        <table class="table table-sortable">
          <thead>
          <tr>
            <th class="col-sm-2 sortable">Name</th>
            <th class="col-sm-1 sortable">Sequence</th>
            <th class="col-sm-2 sortable">Assessment Type</th>
            <th class="col-sm-2 sortable">Weighting</th>
          </tr>
          </thead>
          <tbody>
            <#list assessmentComponents as assessmentComponent>
              <tr>
                <td>${assessmentComponent.name}</td>
                <td>${assessmentComponent.sequence}</td>
                <td>
                  <select class="form-control update-assessment-type" data-sequence="${assessmentComponent.sequence}">
                    <#list assessmentTypes as at><option value="${at.astCode}">${at.name}</option></#list>
                  </select>
                </td>
                <td>${assessmentComponent.scaledWeighting}</td>
              </tr>
            </#list>
          </tbody>
        </table>

        <#list requiresResits as record>
          <#assign member = membersBySprCode[record.module.sprCode] />
          <#assign sprCode = record.module.sprCode />
          <section class="resit-student panel panel-default">
            <div class="panel-heading">
                <@pl.profile_link member.universityId /> ${sprCode} &mdash; ${member.firstName} ${member.lastName}
            </div>
            <div class="panel-body">
              <div class="row">
                <div class="col-sm-2">
                  <@detail "Module mark">${record.module.mark!'-'}</@detail>
                  <@detail "Module grade">${record.module.grade!'-'}</@detail>
                  <@detail "Module result">${(record.module.result.description)!'-'}</@detail>
                </div>
                <div class="col-sm-10">
                  <table class="table table-sortable">
                    <thead>
                      <tr>
                        <th class="col-sm-1"></th>
                        <th class="col-sm-2 sortable">Name</th>
                        <th class="col-sm-1 sortable">Sequence</th>
                        <th class="col-sm-1 sortable">Mark</th>
                        <th class="col-sm-1 sortable">Grade</th>
                        <th class="col-sm-2 sortable">Assessment Type</th>
                        <th class="col-sm-2 sortable">Weighting</th>
                        <th class="col-sm-2">Attempt</th>
                      </tr>
                    </thead>
                    <tbody>
                      <#list record.components?keys as assessmentComponent>
                        <#assign componentRecord = mapGet(record.components, assessmentComponent) />
                        <tr>
                          <td>
                            <#if componentRecord.existingResit?has_content>
                              <span class="label label-success tabula-tooltip" data-title="Resit - ${componentRecord.existingResit.resitSequence}">
                                Resit created
                                <span class="sr-only">Resit - ${componentRecord.existingResit.resitSequence}</span>
                              </span>
                              <#if componentRecord.existingResit.needsWritingToSits>
                                <span class="tabula-tooltip" data-title="Waiting to be written to SITS">
                                  <i class="fa-fw fad fa-cloud-upload" aria-hidden="true"></i>
                                  <span class="sr-only">Waiting to be written to SITS</span>
                                </span>
                              </#if>
                            <#elseif record.requiresResit>
                              <@f.checkbox path="resits[${sprCode}][${assessmentComponent.sequence}].create" checked="checked" />
                            </#if>
                          </td>
                          <td>${assessmentComponent.name}</td>
                          <td>${assessmentComponent.sequence}</td>
                          <td>${componentRecord.mark!'-'}</td>
                          <td>${componentRecord.grade!'-'}</td>
                          <td>
                            <#if componentRecord.existingResit?has_content>
                              ${componentRecord.existingResit.assessmentType.name}
                            <#elseif record.requiresResit>
                              <select name="resits[${sprCode}][${assessmentComponent.sequence}].assessmentType" class="form-control assessment-type" data-sequence="${assessmentComponent.sequence}">
                                <#list assessmentTypes as at><option value="${at.astCode}" >${at.name}</option></#list>
                              </select>
                             <#else>
                                ${assessmentComponent.assessmentType.name}
                             </#if>
                          </td>
                          <td>${assessmentComponent.scaledWeighting}</td>
                          <td><#if record.incrementsAttempt>${record.currentResitAttempt!1 + 1}<#else>${componentRecord.currentResitAttempt!1}</#if></td>
                        </tr>
                      </#list>
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          </section>
        </#list>
    <#else>
      <p>No students taking this module are expected to resit.</p>
    </#if>

  <div class="submit-buttons fix-footer">
    <#if requiresResits?has_content && !hasErrors><input type="submit" class="btn btn-primary" value="Confirm" /></#if>
    <a class="btn btn-default dirty-check-ignore" href="${cancelUrl?markup_string}">Cancel</a>
  </div>
</@f.form>
