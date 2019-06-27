<#escape x as x?html>

  <#if !isSelf>
    <details class="indent">
      <summary>${member.fullName}</summary>
      <#if member.userId??>
        ${member.userId}<br />
      </#if>
      <#if member.email??>
        <a href="mailto:${member.email}">${member.email}</a><br />
      </#if>
      <#if member.phoneNumber??>
        ${phoneNumberFormatter(member.phoneNumber)}<br />
      </#if>
      <#if member.mobileNumber??>
        ${phoneNumberFormatter(member.mobileNumber)}<br />
      </#if>
    </details>
  </#if>

  <h1>Modules</h1>

  <#if hasPermission>

    <#if user.staff>
      <div class="pull-right">
        <@routes.profiles.mrm_link command.studentCourseYearDetails />
        View in MRM<img class="targetBlank" alt="" title="Link opens in a new window" src="/static/images/shim.gif" />
        </a>
      </div>
    </#if>

    <p>Module Registration Status:
      <#if command.studentCourseYearDetails.moduleRegistrationStatus??>
        ${(command.studentCourseYearDetails.moduleRegistrationStatus.description)!}
      <#else>
        Unknown (not in SITS)
      </#if>
    </p>

    <h3>
      <strong>Year mark:</strong> ${yearMark!"-"}
      <#if (weightedMeanYearMark!"-")?string != (yearMark!"-")?string>
        (weighted mean: ${weightedMeanYearMark!"-"})
      </#if>
      |
      <strong>Year Weighting:</strong>
      <#if yearWeighting??>${yearWeighting.weightingAsPercentage}%<#else>-</#if>
    </h3>

    <#if moduleRegistrationsAndComponents?has_content>
      <#assign showModuleResults = features.showModuleResults />
      <#list moduleRegistrationsAndComponents as moduleRegistrationAndComponent>
        <#assign moduleRegistration = moduleRegistrationAndComponent.moduleRegistration />
        <div class="striped-section collapsible">
          <h3 class="section-title">
            <a class="collapse-trigger icon-container" href="#">
              <@fmt.module_name moduleRegistration.module />
              <span class="mod-reg-summary">
              <#if showModuleResults>
                <span class="mod-reg-summary-item"><strong>CATS:</strong> ${(moduleRegistration.cats)!}</span>
                <span class="mod-reg-summary-item"><strong>Mark:</strong> ${(moduleRegistration.agreedMark)!"-"}</span>
                <#if moduleRegistration.agreedGrade??><span class="mod-reg-summary-item"><strong>Grade:</strong> ${(moduleRegistration.agreedGrade)!}</span></#if>
                <#if moduleRegistration.passedCats??>
                <span class="mod-reg-summary-item"><strong>Passed CATS:</strong> <#if moduleRegistration.passedCats>${(moduleRegistration.cats)!}<#else>0</#if></span>
              </#if>
              </#if>
            </span>
          </a>
          </h3>
          <div class="striped-section-contents ">
            <div class="item-info">
              <div class="row">
                <div class="col-md-4">
                  <h4><@fmt.module_name moduleRegistration.module false /></h4>
                </div>
                <div class="col-md-4">
                  <strong>Assessment group:</strong> ${(moduleRegistration.assessmentGroup)!} <br />
                  <strong>Occurrence:</strong> ${(moduleRegistration.occurrence)!} <br />
                  <strong>Status:</strong>
                  <#if moduleRegistration.selectionStatus??>
                    ${(moduleRegistration.selectionStatus.description)!}
                  <#else>
                    -
                  </#if> <br />
                  <strong>CATS:</strong> ${(moduleRegistration.cats)!} <br />
                </div>
                <div class="col-md-4">
                  <#if showModuleResults>
                    <strong>Mark:</strong> ${(moduleRegistration.agreedMark)!"-"} <br />
                    <strong>Grade:</strong> ${(moduleRegistration.agreedGrade)!"-"} <br />
                    <strong>Passed CATS:</strong>
                    <#if moduleRegistration.passedCats??>
                      <#if moduleRegistration.passedCats>${(moduleRegistration.cats)!}<#else>0</#if>
                    <#else>
                      -
                    </#if><br />
                  </#if>
                </div>
              </div>
              <div class="row">
                <div class="col-md-12">
                  <strong>Components:</strong><br />
                  <table class="table table-condensed table-striped">
                    <thead>
                    <tr>
                      <th>Sequence</th>
                      <th>Name</th>
                      <th>Weighting</th>
                      <th>Mark</th>
                      <th>Grade</th>
                    </tr>
                    </thead>
                    <tbody>
                    <#list moduleRegistrationAndComponent.components as component>
                      <tr>
                        <td>${component.upstreamGroup.sequence}</td>
                        <td>${component.upstreamGroup.name}</td>
                        <td>${component.upstreamGroup.assessmentComponent.weighting!0}%</td>
                        <td>${component.member.firstAgreedMark!}</td>
                        <td>${component.member.firstAgreedGrade!}</td>
                      </tr>
                    </#list>
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          </div>
        </div>
      </#list>

    <#else>

      <div class="alert alert-info">
        There are no module registrations for this academic year.
      </div>

    </#if>

  <#else>

    <div class="alert alert-info">
      You do not have permission to see the module registrations for this course.
    </div>

  </#if>



</#escape>
