<#import "*/cm2_macros.ftl" as cm2 />
<#if detail.extension?has_content>

  <#if detail.extension.requestedOn?has_content>
    <div>
      <label>Request received:</label> <@fmt.date date=detail.extension.requestedOn />
    </div>
  <#else>
    <#if (modifyExtensionCommand.e?has_content) >
      <#if (modifyExtensionCommand.e._state.description != "Revoked")>
        <div>
        </div>
      </#if>
    <#else>
      <div>
        <label>Manually granted:</label> <@fmt.date date=detail.extension.reviewedOn />
      </div>
    </#if>
  </#if>

  <#if detail.extension.requestedExpiryDate?has_content>
    <div>
      <label>Requested extension length:</label>
      <@fmt.p detail.extension.requestedExtraDuration "day"/> past the deadline.
      <@fmt.date date=detail.extension.requestedExpiryDate />
    </div>
  </#if>
  <#if detail.extension.feedbackDueDate?has_content>
    <div>
      <label>Feedback due:</label> <@fmt.date date=detail.extension.feedbackDueDate />
    </div>
  </#if>
  <div>
    <label>Previous extension requests:</label>
    <#if detail.previousExtensions?has_content>
      <a href="" data-toggle="modal" data-target="#prev-extensions-${detail.extension.id}">
        ${detail.previousExtensions?size}
      </a>
    <#else>
      0
    </#if>
  </div>
  <div>
    <label>Previous submissions:</label>
    <#if detail.previousSubmissions?has_content>
      <a href="" data-toggle="modal" data-target="#prev-submissions-${detail.extension.id}">
        ${detail.previousSubmissions?size}
      </a>
    <#else>
      0
    </#if>
  </div>


  <details>
    <summary>About this student (${student.universityId})</summary>
    <dl class="unstyled">
      <#if (studentContext.course)?has_content>
        <#assign c = studentContext.course />
        <dt>Course details</dt>
        <dd>
          <@fmt.course_description c />
          <span class="muted">${(c.latestStudentCourseYearDetails.modeOfAttendance.fullNameToDisplay)!}</span>
        </dd>
      </#if>
      <#if (studentContext.relationships)?has_content>
        <dt>Relationships</dt>
        <dd>
          <#assign rels = studentContext.relationships />
          <ul class="unstyled">
            <#list rels?keys as key>
              <#list rels[key] as agent>
                <li>
                  ${agent.agentMember.fullName}, ${agent.agentMember.description} &ensp;<span class="muted">${key}</span>
                </li>
              </#list>
            </#list>
          </ul>
        </dd>
      </#if>

      <#if student?has_content>
        <#if student.mobileNumber??>
          <dt>Mobile number</dt>
          <dd>${student.mobileNumber}</dd></#if>
        <#if student.phoneNumber?? && student.phoneNumber != student.mobileNumber!"">
          <dt>Telephone number</dt>
          <dd>${student.phoneNumber}</dd></#if>
        <#if student.email??>
          <dt>Email address</dt>
          <dd>${student.email}</dd></#if>
      <#else>
        <div class="alert alert-info">
          Further details for this user are not available in Tabula.
        </div>
      </#if>

    </dl>
  </details><br />

  <#if detail.extension.reason?has_content>
    <details>
      <summary>Reason for request</summary>

      <textarea class="form-control" rows="3" disabled="disabled">${detail.extension.reason}</textarea>

      <#if detail.extension.attachments?has_content>
        <label>Supporting documents</label>
        <ul>
          <#list detail.extension.attachments as attachment>
            <li>
              <a href="<@routes.cm2.extensionAttachment detail.extension attachment.name />">
                ${attachment.name}
              </a>
            </li>
          </#list>
        </ul>
      </#if>
    </details>
  </#if>

  <#assign formAction><@routes.cm2.extensionUpdate detail.extension /></#assign>

  <@f.form
  method="post"
  class="modify-extension double-submit-protection"
  action="${formAction}"
  modelAttribute="editExtensionCommand"
  >
  </@f.form>

  <@cm2.previousExtensions detail.extension.id detail.studentIdentifier detail.student.fullName detail.numAcceptedExtensions detail.numRejectedExtensions detail.previousExtensions />
  <@cm2.previousSubmissions detail.extension.id detail.studentIdentifier detail.student.fullName detail.previousSubmissions />

  <#assign feedbackNotice>
    <#if detail.extension.approved>
      <#if detail.extension.feedbackDeadline?has_content>
        <br />Feedback for this student is currently due <@fmt.date date=detail.extension.feedbackDeadline capitalise=false at=true />.
      <#else>
        <br />Feedback for this student has no due date.
      </#if>
    </#if>
  </#assign>
</#if>

<#escape x as x?html>
  <div class="content extension-detail">
    <#assign actionUrl><@routes.cm2.extensiondetail assignment usercode /></#assign>
    <@f.form method="post" enctype="multipart/form-data" action=actionUrl id="editExtensionCommand" modelAttribute="editExtensionCommand" cssClass="form-horizontal double-submit-protection modify-extension">
      <input type="hidden" name="closeDate" class="startDateTime" value="${assignment.closeDate!""}" />

      <#if detail.extension?has_content>
        <#if detail.extension.awaitingReview>
          <input type="hidden" name="rawRequestedExpiryDate" value="${detail.extension.requestedExpiryDate}" />
          <h5>Requested <@fmt.date date=detail.extension.requestedExpiryDate capitalise=false at=true/>&ensp;
          <#if assignment.closeDate??>
            <span class="muted">${durationFormatter(assignment.closeDate, detail.extension.requestedExpiryDate)} after the set deadline</span></h5>
          </#if>
          <#if detail.extension.approved>
            <p>
              <b>This is a revised request from the student.</b>
              There is already an extension approved until <@fmt.date date=detail.extension.expiryDate capitalise=false at=true/>
              <#if assignment.closeDate??>
                (${durationFormatter(assignment.closeDate, detail.extension.expiryDate)} after the set deadline).
              </#if>
              <#noescape>${feedbackNotice}</#noescape>
            </p>
            <p class="alert alert-info">To retain the existing extension, choose <i>Update</i> below,
              leaving a comment for the student if you wish.<br><i>Reject</i> will remove the existing extension as well.</p>
          <#elseif detail.extension.rejected>
            <p><b>This is a revised request from the student.</b> An earlier request was rejected
              <@fmt.date date=detail.extension.reviewedOn capitalise=false includeTime=false />.</p>
          </#if>
        <#elseif detail.extension.initiatedByStudent>
          <#if detail.extension.approved && detail.extension.expiryDate?has_content >
            <h5>Approved <@fmt.date date=detail.extension.reviewedOn capitalise=false includeTime=false />
            until <@fmt.date date=detail.extension.expiryDate capitalise=false at=true />&ensp;
            <#if assignment.closeDate??>
              <span class="muted">${durationFormatter(assignment.closeDate, detail.extension.expiryDate)} after the set deadline</span></h5>
            </#if>
          <#elseif detail.extension.rejected>
            <h5>Rejected <@fmt.date date=detail.extension.reviewedOn capitalise=false includeTime=false /></h5>
          </#if>
          <p>
            <#if (detail.extension.expiryDate?has_content)&&(detail.extension.requestedExpiryDate?has_content)&&(detail.extension.expiryDate != detail.extension.requestedExpiryDate)>
              The extension was requested for <@fmt.date date=detail.extension.requestedExpiryDate capitalise=false at=true />
              <#if assignment.closeDate??>
                (${durationFormatter(assignment.closeDate, detail.extension.requestedExpiryDate)} after the set deadline).
              </#if>
              <#noescape>${feedbackNotice}</#noescape>
            </#if>
          </p>
        <#elseif detail.extension.approved>
          <p><#noescape>${feedbackNotice}</#noescape></p>
        </#if>

        <#if features.disabilityRenderingInExtensions && detail.extension.disabilityAdjustment && student?? && (student.disability.reportable)!false && can.do("Profiles.Read.Disability", student)>
          <p>${student.firstName} has requested their ${(student.disability.definition)!"recorded disability"} be taken into consideration.</p>
        </#if>

      </#if>
      <@bs3form.labelled_form_group "expiryDate" "Extended deadline">
        <div class="input-group">
          <@f.input path="expiryDate" cssClass="form-control date-time-minute-picker" />
          <span class="input-group-addon"><i class="fa fa-calendar"></i></span>
        </div>
      </@bs3form.labelled_form_group>

      <@bs3form.labelled_form_group "reviewerComments" "Comments">
        <@f.textarea path="reviewerComments" cssClass="form-control text big-textarea" maxlength=4000/>
        <div class="muted">Any comments will be saved and sent to the student</div>
      </@bs3form.labelled_form_group>

      <@bs3form.errors "state"/>

      <div class="submit-buttons">
        <#if detail.extension?has_content>
          <#if detail.extension.approved>
            <button type="submit" name="state" value="${editExtensionCommand.state.dbValue}" class="btn btn-primary">Update</button>
            <button type="submit" name="state" value="${states.Revoked.dbValue}" class="btn btn-default">Revoke</button>
          <#elseif detail.extension.rejected || detail.extension.revoked>
            <button type="submit" name="state" value="${editExtensionCommand.state.dbValue}" class="btn btn-primary">Update</button>
            <button type="submit" name="state" value="${states.Approved.dbValue}" class="btn btn-primary">Approve</button>
          <#elseif detail.extension.moreInfoRequired>
            <button type="submit" name="state" value="${editExtensionCommand.state.dbValue}" class="btn btn-default">Update</button>
            <button type="submit" name="state" value="${states.Approved.dbValue}" class="btn btn-primary">Approve</button>
            <button type="submit" name="state" value="${states.Rejected.dbValue}" class="btn btn-default">Reject</button>
          <#elseif detail.extension.unreviewed || detail.extension.moreInfoReceived>
            <button type="submit" name="state" value="${states.Approved.dbValue}" class="btn btn-primary">Approve</button>
            <button type="submit" name="state" value="${states.Rejected.dbValue}" class="btn btn-default">Reject</button>
            <button type="submit" name="state" value="${states.MoreInformationRequired.dbValue}" class="btn btn-default">Request more information</button>
          </#if>
        <#elseif can.do('Extension.Create', assignment)>
          <button class="btn btn-primary" name="state" value="${states.Approved.dbValue}" type="submit">Approve</button>
        </#if>
        <a class="btn btn-default discard-changes" href="">Discard changes</a>
      </div>
    </@f.form>
  </div>
</#escape>