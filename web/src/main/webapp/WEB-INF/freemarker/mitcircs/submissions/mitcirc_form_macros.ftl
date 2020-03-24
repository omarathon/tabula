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

<#assign questionIndex = 1 /> <#-- Used to generate unique IDs for popups - not used as the question number -->
<#macro question_section question="" covidQuestion=""  hint="" covidHint="" cssClass="" helpPopover="" helpCovidPopover="" showNumber=true data="" covid19Hide=false>
  <fieldset class="mitcircs-form__fields__section mitcircs-form__fields__section--boxed ${cssClass}" ${data} <#if features.mitcircsCovid19 && covid19Hide>style="display: none;"</#if>>
    <#if question?has_content>
      <legend>
        <#if showNumber><span class="mitcircs-form__fields__section__number"></span>.</#if>
        <span <#if covidQuestion?has_content>class="covid--no"</#if>>${question}</span>
        <#if covidQuestion?has_content><span class="covid--yes">${covidQuestion}</span></#if>

        <#if helpPopover?has_content>
          <#if helpCovidPopover?has_content><#assign covidClass>covid--no</#assign></#if>
          <@fmt.help_popover id="question_section${questionIndex}_popover" content="${helpPopover}" html=true placement="left" cssClass="${covidClass}" />
        </#if>
        <#if helpCovidPopover?has_content>
          <@fmt.help_popover id="question_section${questionIndex}_covid-popover" content="${helpCovidPopover}" html=true placement="left" cssClass="covid--yes" />
        </#if>
      </legend>
    </#if>

    <#if hint?has_content>
      <@question_hint hint covidHint?has_content />
    </#if>

    <#if covidHint?has_content>
      <@question_covid_hint covidHint />
    </#if>

    <#nested />
  </fieldset>
  <#assign questionIndex = questionIndex + 1 />
</#macro>

<#macro question_hint hint covidHint=false>
  <p class="mitcircs-form__fields__section__hint <#if covidHint> covid--no</#if>" >(${hint})</p>
</#macro>

<#macro question_covid_hint hint>
  <p class="mitcircs-form__fields__section__hint covid--only covid--yes">(${hint})</p>
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
        <#if value.covidHelpText??><@fmt.help_popover id="${value.entryName}Covid" content="${value.covidHelpText}" placement="left" cssClass="covid--yes"/></#if>
      </#if>
      <#if value.helpText??>
        <#assign covidClass><#if value.covidHelpText??>covid--no</#if></#assign>
        <@fmt.help_popover id="${value.entryName}" content="${value.helpText}" placement="left" cssClass="${covidClass}" />
      </#if>
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