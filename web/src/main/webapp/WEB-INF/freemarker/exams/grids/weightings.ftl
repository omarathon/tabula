<#escape x as x?html>
  <#import "*/modal_macros.ftlh" as modal />

  <div class="pull-right">
    <button class="btn btn-default" data-toggle="modal" data-target="#copy-weightings-modal">
      Copy from other academic year
    </button>
  </div>

  <@modal.modal id="copy-weightings-modal">
    <@modal.wrapper cssClass="modal-lg">
      <@modal.header>
        <h3 class="modal-title">Copy from other academic year</h3>
      </@modal.header>
      <@modal.body>
        <div class="alert alert-info">
          Copying from another academic year will overwrite any changes you have made but not saved,
          but you can review and update the copy before it is saved.
        </div>
        <@bs3form.labelled_form_group path="" labelText="Academic year">
          <select class="form-control" name="academicYear" data-href="<@routes.exams.manageWeightings department academicYear />/fetch/">
            <option value="" disabled selected></option>
            <#list availableAcademicYears as academicYear>
              <option value="${academicYear.startYear?c}">${academicYear.toString}</option>
            </#list>
          </select>
        </@bs3form.labelled_form_group>
        <div class="loading hide">
          <i class="fa fa-spinner fa-spin"></i><em> Loading&hellip;</em>
        </div>
        <div class="content"></div>
      </@modal.body>
      <@modal.footer>
        <div class="submit-buttons">
          <button class="btn btn-primary" name="copy" disabled>Copy</button>
          <button class="btn btn-default" data-dismiss="modal">Cancel</button>
        </div>
      </@modal.footer>
    </@modal.wrapper>
  </@modal.modal>

  <#function route_function dept>
    <#local result><@routes.exams.manageWeightings dept academicYear /></#local>
    <#return result />
  </#function>

  <@fmt.id7_deptheader title="Manage course year weightings for ${academicYear.toString}" route_function=route_function preposition="in" />

  <p>
    Course year weightings are the percentage of a final degree mark contributed by each year.
    These are not imported from SITS. The weighting for each year must be specified in Tabula to
    generate a final degree mark in an exam grid. The weighting chosen corresponds to the student's
    SPR (Student Programme Route) start year, and is specified for each year of study on their course.
  </p>
  <p>
    Enter weightings as a percentage. These usually total 100 for a degree, though weightings for
    courses with a year abroad can exceed 100. Tabula disregards the year abroad and associated
    weighting when calculating the final degree mark.
  </p>

  <#assign formUrl><@routes.exams.manageWeightings department academicYear /></#assign>

  <div class="fix-area course-weightings-editor">

    <@f.form method="post" action="${formUrl}" modelAttribute="command" cssClass="dirty-check">

      <@spring.bind path="command">
        <#if (status.errors.allErrors?size > 0)>
          <div class="alert alert-danger">
            <@f.errors path="*" />
          </div>
        </#if>
      </@spring.bind>

      <div class="header fix-header">
        <div class="row">
          <div class="col-xs-9 col-xs-offset-2">
            <div class="pull-right">
              <div class="checkbox">
                <label>
                  <input name="showAdditionalYears" type="checkbox" />
                  Show additional years of study
                </label>
              </div>
            </div>
            <label>Year of study</label>
          </div>
        </div>
        <div class="row last">
          <div class="col-xs-2">
            <label>Course</label>
          </div>
          <div class="col-xs-9">
            <#list allYearsOfStudy as year>
              <div class="col-xs-1"><label>${year}</label></div>
            </#list>
          </div>

          <div class="col-xs-2">
            <input type="text" class="form-control" name="filter" placeholder="Filter courses" />
          </div>
          <div class="col-xs-9">
            <#list allYearsOfStudy as year>
              <div class="col-xs-1">
                <input type="text" class="form-control" name="bulk" data-year="${year}" placeholder="All" />
              </div>
            </#list>
          </div>
          <div class="col-xs-1">
            <button type="button" name="bulkapply" class="btn btn-default btn-sm" disabled>Apply</button>
            <@fmt.help_popover id="bulkapply" content="Enter a weighting and click Apply to change all filtered courses" />
          </div>
        </div>
      </div>

      <#list command.allCourses as course>
        <div class="row">
          <div class="col-xs-2">
            <label tabindex="0" title="${course.code?upper_case} ${course.name}" class="use-tooltip">${course.code?upper_case} ${course.name}</label>
          </div>
          <div class="col-xs-9">
            <#list allYearsOfStudy as year>
              <#assign value = "" />
              <#if mapGet(command.yearWeightings, course)?? && mapGet(mapGet(command.yearWeightings, course), year)?? >
                <#assign value = mapGet(mapGet(command.yearWeightings, course), year) />
              </#if>
              <div class="col-xs-1">
                <input title="${course.code?upper_case} ${course.name} Year ${year}"
                       data-container="body"
                       type="text"
                       class="use-tooltip form-control"
                       name="yearWeightings[${course.code}][${year}]"
                       value="${value}"
                />
              </div>
            </#list>
          </div>
        </div>
      </#list>

      <div class="submit-buttons fix-footer">
        <button class="btn btn-primary" type="submit" name="confirm">Save</button>
        <a class="btn btn-default dirty-check-ignore" href="<@routes.exams.gridsDepartmentHomeForYear department academicYear />">Cancel</a>
      </div>
    </@f.form>

  </div>

  <script nonce="${nonce()}">
    window.ExamGrids.manageWeightings();
  </script>

</#escape>