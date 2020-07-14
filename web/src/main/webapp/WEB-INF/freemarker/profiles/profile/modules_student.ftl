<#import "*/profiles_macros.ftl" as profiles />
<#escape x as x?html>

  <@profiles.profile_header member isSelf />

  <h1>Modules</h1>

  <#if hasPermission>

    <#if user.staff>
      <div class="pull-right">
        <@routes.profiles.mrm_link command.studentCourseYearDetails />
        View in MRM<img class="targetBlank" alt="" title="Link opens in a new window" src="/static/images/shim.gif" />
        </a>
      </div>

      <p>Module Registration Status:
        <#if command.studentCourseYearDetails.moduleRegistrationStatus??>
          ${(command.studentCourseYearDetails.moduleRegistrationStatus.description)!}
        <#else>
          Unknown (not in SITS)
        </#if>
      </p>
    </#if>

    <h3>
      <#if moduleRegistrationsAndComponents?has_content>
        <#assign totalCats = 0 />
        <#list moduleRegistrationsAndComponents as mrc>
          <#assign totalCats = totalCats + (mrc.moduleRegistration.cats!0) />
        </#list>
        <strong>Total CATS:</strong> ${totalCats}
      </#if>
     <#if !hideYearWeightings || (user.staff && !isSelf)>
        <strong>Year mark:</strong> ${yearMark!"-"}
        <#if (weightedMeanYearMark!"-")?string != (yearMark!"-")?string>
          (weighted mean: ${weightedMeanYearMark!"-"})
        </#if>
        <strong>Year Weighting:</strong>
        <#if yearAbroad>0%<#else><#if yearWeighting??>${yearWeighting.weightingAsPercentage}%<#else>-</#if></#if>
     </#if>
    </h3>

    <#if progressionDecisions?has_content>
      <#list progressionDecisions as progressionDecision>
        <div class="panel panel-default">
          <div class="panel-heading">
            <h3 class="panel-title">
              Progression decision
              <#if progressionDecision.resitPeriod>
                <span class="label label-info">Summer vacation exam period</span>
              <#else>
                <span class="label label-info">Summer exam period</span>
              </#if>
            </h3>
          </div>
          <div class="panel-body">
            <div class="lead">${progressionDecision.outcome.message}</div>
          </div>
        </div>
      </#list>
    </#if>

    <#if moduleRegistrationsAndComponents?has_content>
      <#assign showModuleResults = features.showModuleResults />
      <#list moduleRegistrationsAndComponents as moduleRegistrationAndComponent>
        <#assign moduleRegistration = moduleRegistrationAndComponent.moduleRegistration />
        <div class="striped-section collapsible">
          <h3 class="section-title">
            <a class="collapse-trigger icon-container" href="#"><#compress>
              <@fmt.module_name moduleRegistration.module />
            </#compress></a>
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
                      <th>Type</th>
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
                        <td>${component.upstreamGroup.assessmentComponent.assessmentType.name}</td>
                        <td>${component.upstreamGroup.name}</td>
                        <td>${(component.weighting!0)?string["0.#"]}%</td>
                        <td>${component.member.agreedMark!}</td>
                        <td>${component.member.agreedGrade!}</td>
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

      <h2>Examinations</h2>

      <table class="table table-striped">
        <thead>
          <tr>
            <th>Module</th>
            <th>Paper code</th>
            <th>Paper title</th>
            <th>Section</th>
            <th>Duration</th>
            <th>Reading time</th>
            <th>Exam type</th>
            <th>Assessment type</th>
          </tr>
        </thead>
        <tbody>
          <#list moduleRegistrationsAndComponents as moduleRegistrationAndComponent>
            <#list moduleRegistrationAndComponent.components as component>
              <#if component.upstreamGroup.assessmentComponent.assessmentType.subtype.code == 'E' && component.upstreamGroup.assessmentComponent.assessmentType.astCode != 'LX' && component.upstreamGroup.assessmentComponent.assessmentType.astCode != 'HE' && component.upstreamGroup.assessmentComponent.assessmentType.astCode != 'OE'>
                <tr>
                  <td>${moduleRegistrationAndComponent.moduleRegistration.module.code?upper_case}<#if moduleRegistrationAndComponent.moduleRegistration.cats?has_content>-${moduleRegistrationAndComponent.moduleRegistration.cats}</#if></td>
                  <#if component.upstreamGroup.assessmentComponent.examPaperCode?has_content>
                    <td>${component.upstreamGroup.assessmentComponent.examPaperCode}</td>
                    <td>${component.upstreamGroup.assessmentComponent.examPaperTitle!""}</td>
                    <td>${component.upstreamGroup.assessmentComponent.examPaperSection!'Unknown'}</td>
                    <td>
                      <#if component.upstreamGroup.assessmentComponent.examPaperDuration??>
                        ${durationFormatter(component.upstreamGroup.assessmentComponent.examPaperDuration)}
                      <#else>
                        Unknown
                      </#if>
                    </td>
                    <td>
                      <#if component.upstreamGroup.assessmentComponent.examPaperReadingTime??>
                        ${durationFormatter(component.upstreamGroup.assessmentComponent.examPaperReadingTime)}
                      <#else>
                        n/a
                      </#if>
                    </td>
                    <td>
                      <#if component.upstreamGroup.assessmentComponent.examPaperType??>
                        ${component.upstreamGroup.assessmentComponent.examPaperType.name}
                        <#if component.upstreamGroup.assessmentComponent.examPaperType.description??>
                          <a class="use-popover" data-trigger="click focus" data-placement="left" data-content="${component.upstreamGroup.assessmentComponent.examPaperType.description}">
                            <i class="fal fa-question-circle"></i>
                          </a>
                        </#if>
                      <#else>
                        Unknown
                      </#if>
                    </td>
                  <#else>
                    <td colspan="6">Unknown</td>
                  </#if>
                  <td>${component.upstreamGroup.assessmentComponent.assessmentType.name}</td>
                </tr>
              </#if>
            </#list>
          </#list>
        </tbody>
      </table>



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
