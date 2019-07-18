<#escape x as x?html>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>

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
<#macro question_section question="" hint="" cssClass="" helpPopover="" showNumber=true data="">
  <fieldset class="mitcircs-form__fields__section mitcircs-form__fields__section--boxed ${cssClass}" ${data}>
    <#if question?has_content>
      <legend>
        <#if showNumber>${questionNumber}.</#if> ${question}
        <#if helpPopover?has_content>
          <a class="help-popover use-popover tabulaPopover-init" id="popover-evidence" data-html="true" data-placement="left" data-content="${helpPopover}" data-container="body" aria-label="Help" data-original-title="" title="">
            <i class="icon-question-sign fa fa-question-circle"></i>
          </a>
        </#if>
      </legend>
      <#if showNumber><#assign questionNumber = questionNumber + 1 /></#if>
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

<#macro checkboxes enumValues enumField>
  <#list enumValues as value>
    <div class="checkbox">
      <label>
        <@f.checkbox path="${enumField}" value="${value.entryName}" /> ${value.description}
      </label>
    </div>
  </#list>
  <@bs3form.errors path="${enumField}" />
</#macro>

<#macro checkboxesWithOther enumValues enumField otherField additionalDescriptions = {}>
  <#list enumValues as value>
    <div
      class="checkbox clearfix <#if value.entryName == "Other">mitcircs-form__fields__checkbox-with-other</#if>"
      <#if value.evidenceGuidance??>data-evidenceguidance="${value.evidenceGuidance}"</#if>
    >
      <label>
        <@f.checkbox path="${enumField}" value="${value.entryName}" /> ${value.description}
        <#if additionalDescriptions[value.entryName]?? >- ${additionalDescriptions[value.entryName]}</#if>
      </label>
      <#if value.entryName == "Other">
        <@f.input path="${otherField}" cssClass="form-control other-input" />
      </#if>
      <#if value.helpText??><@fmt.help_popover id="${value.entryName}" content="${value.helpText}" placement="left"/></#if>
    </div>
    <#if value.entryName == "Other"><@bs3form.errors path="${otherField}" /></#if>
  </#list>
  <@bs3form.errors path="${enumField}" />
</#macro>

<#macro radios enumValues enumField>
  <div class="enum-radios">
    <#list enumValues as value>
      <div class="radio">
        <label>
          <@f.radiobutton path="${enumField}" value="${value.entryName}" /> ${value.description}
        </label>
        <#if value.helpText??><@fmt.help_popover id="${value.entryName}" content="${value.helpText}" placement="left"/></#if>
      </div>
    </#list>
  </div>
  <@bs3form.errors path="${enumField}" />
</#macro>
</#escape>