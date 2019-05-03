<#macro identity_info key value>
  <#if value?has_content>
    <div class="row form-horizontal">
      <div class="col-sm-3 control-label">${key}</div>
      <div class="col-sm-9">
        <div class="form-control-static">${value}</div>
      </div>
    </div>
  </#if>
</#macro>

<#macro identity_section student>
  <fieldset class="mitcircs-form__fields__section mitcircs-form__fields__section--identity">
    <@identity_info "Name" student.fullName />
    <@identity_info "University ID" student.universityId />
    <#if student.email??><@identity_info "Email" student.email /></#if>

    <#if student.mostSignificantCourseDetails??>
      <#local studentCourseDetails = student.mostSignificantCourseDetails />
      <@identity_info "Course" studentCourseDetails.course.name />

      <#if studentCourseDetails.latestStudentCourseYearDetails??>
        <#local studentCourseYearDetails = studentCourseDetails.latestStudentCourseYearDetails />

        <#if studentCourseYearDetails.yearOfStudy??>
          <@identity_info "Year of study" studentCourseYearDetails.yearOfStudy />
        </#if>

        <#if studentCourseYearDetails.modeOfAttendance??>
          <@identity_info "Mode of study" studentCourseYearDetails.modeOfAttendance.fullNameAliased />
        </#if>
      </#if>
    </#if>

    <#if submission??>
      <@identity_info "Reference" "MIT-${submission.key}" />
    </#if>
  </fieldset>
</#macro>

<#assign questionNumber = 1 />
<#macro question_section question="" hint="" cssClass="" helpPopover="">
  <fieldset class="mitcircs-form__fields__section mitcircs-form__fields__section--boxed ${cssClass}">
    <#if question?has_content>
      <legend>
        ${questionNumber}. ${question}
        <#if helpPopover?has_content>
          <a class="help-popover use-popover tabulaPopover-init" id="popover-evidence" data-html="true" data-placement="left" data-content="${helpPopover}" data-container="body" aria-label="Help" data-original-title="" title="">
            <i class="icon-question-sign fa fa-question-circle"></i>
          </a>
        </#if>
      </legend>
      <#assign questionNumber = questionNumber + 1 />
    </#if>

    <#if hint?has_content>
      <@question_hint hint />
    </#if>

    <#nested />
  </fieldset>
</#macro>

<#macro question_hint hint>
  <p class="mitcircs-form__fields__section__hint">(${hint})</p>
</#macro>