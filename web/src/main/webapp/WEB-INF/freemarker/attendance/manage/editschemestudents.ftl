<#escape x as x?html>
  <#import "*/attendance_macros.ftl" as attendance_macros />
  <#import "*/csrf_macros.ftl" as csrf_macros />

  <#macro listStudentIdInputs>
    <#list findCommand.staticStudentIds as id>
      <input type="hidden" name="staticStudentIds" value="${id}" />
    </#list>
    <#list findCommand.includedStudentIds as id>
      <input type="hidden" name="includedStudentIds" value="${id}" />
    </#list>
    <#list findCommand.excludedStudentIds as id>
      <input type="hidden" name="excludedStudentIds" value="${id}" />
    </#list>
  </#macro>

  <h1>Edit scheme: ${scheme.displayName}</h1>

  <form class="add-student-to-scheme" method="POST">
    <@csrf_macros.csrfHiddenInputField />
    <input type="hidden" name="filterQueryString" value="${findCommand.serializeFilter}" />
    <@listStudentIdInputs />

    <p class="progress-arrows">
      <span tabindex="0" class="arrow-right use-tooltip" title="Save and edit properties"><button type="submit" class="btn btn-link"
                                                                        name="${ManageSchemeMappingParameters.saveAndEditProperties}">Properties</button></span>
      <span tabindex="0" class="arrow-right arrow-left use-tooltip active">Students</span>
      <span tabindex="0" class="arrow-right arrow-left use-tooltip" title="Save and edit points"><button type="submit" class="btn btn-link"
                                                                                            name="${ManageSchemeMappingParameters.createAndAddPoints}">Points</button></span>
    </p>

    <div class="fix-area">

      <#include "_selectstudents.ftl" />

      <div class="fix-footer submit-buttons">
        <p style="padding-left: 20px;" class="checkbox">
          <label>
            <#if SITSInFlux>
            <input type="checkbox" name="_linkToSits" value="on" disabled />
            Update student list from SITS
            <#assign popoverContent><#noescape>
              You can no longer link to SITS for the current academic year,
              as changes for the forthcoming academic year are being made that will make the students on this scheme inaccurate.
            </#noescape></#assign>
            <a class="use-popover"
               tabindex="0" role="button"
               id="popover-linkToSits"
               data-content="${popoverContent}"
               data-html="true" aria-label="Help"
            >
              <i class="fa fa-question-circle"></i>
            </a>
            <#else>
            <@f.checkbox path="findCommand.linkToSits" />
            Update student list from SITS
            <#assign popoverContent><#noescape>
        <p>
          Select this option to update the list of students on this monitoring scheme as SITS data changes. For example, if a student withdraws from their
          course, they are removed from the scheme.
        </p>
        <p>
          Deselect this option to keep the list updated manually.
        </p>
        </#noescape></#assign>
        <a class="use-popover"
           tabindex="0" role="button"
           id="popover-linkToSits"
           data-content="${popoverContent}"
           data-html="true" aria-label="Help"
        >
          <i class="fa fa-question-circle"></i>
        </a>
        </#if>
        </label>
        </p>

        <#if findCommand.linkToSits>
          <@unlinkSitsAlert />
        </#if>

        <p>
          <input
                  type="submit"
                  class="btn btn-primary use-tooltip spinnable spinner-auto"
                  name="${ManageSchemeMappingParameters.createAndAddPoints}"
                  value="Save and edit points"
                  title="Select which monitoring points this scheme should use"
                  data-container="body"
                  data-loading-text="Saving&hellip;"
          />
          <input
                  type="submit"
                  class="btn btn-primary use-tooltip spinnable spinner-auto"
                  name="persist"
                  value="Save and exit"
                  title="Save your scheme"
                  data-container="body"
                  data-loading-text="Saving&hellip;"
          />

          <a class="btn btn-default" href="<@routes.attendance.manageHomeForYear scheme.department scheme.academicYear />">Cancel</a>
        </p>
      </div>

    </div>

  </form>

</#escape>