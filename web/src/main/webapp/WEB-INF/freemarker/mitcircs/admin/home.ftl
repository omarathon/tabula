<#import "../_filters.ftl" as filters />

<#escape x as x?html>
  <div id="profile-modal" class="modal fade profile-subset"></div>

  <#function route_function dept>
    <#local result><@routes.mitcircs.adminhome dept academicYear /></#local>
    <#return result />
  </#function>
  <@fmt.id7_deptheader "Mitigating Circumstances Submissions" route_function "in" />

<#-- Filtering -->
  <div class="fix-area">
    <div class="fix-header pad-when-fixed">
      <div class="filters mitcircs-submission-filters btn-group-group well well-sm" data-lazy="true">
        <@f.form modelAttribute="command" action="${info.requestedUri.path}" method="GET" cssClass="form-inline">
          <@f.errors cssClass="error form-errors" />

          <#-- Student filters -->
          <div class="mitcircs-submission-filters__student">
            <i class="fal fa-user-graduate fa-fw"></i>

            <#assign placeholder = "All course types" />
            <#assign currentfilter><@filters.current_filter_value "courseTypes" placeholder; courseType>${courseType.code}</@filters.current_filter_value></#assign>
            <@filters.filter "courseTypes" "command.courseTypes" placeholder currentfilter command.allCourseTypes; courseType>
              <input type="checkbox" name="${status.expression}" value="${courseType.code}"
                     data-short-value="${courseType.code}" ${filters.contains_by_code(command.courseTypes, courseType)?string('checked','')}>
              ${courseType.description}
            </@filters.filter>

            <#assign placeholder = "All routes" />
            <#assign currentfilter><@filters.current_filter_value "routes" placeholder; route>${route.code?upper_case}</@filters.current_filter_value></#assign>
            <#assign routesCustomPicker>
              <div class="route-search input-append input-group">
                <input class="route-search-query route prevent-reload form-control" type="text" value="" placeholder="Search for a route" />
                <span class="add-on input-group-addon"><i class="icon-search fa fa-search"></i></span>
              </div>
            </#assign>
            <@filters.filter name="routes" path="command.routes" placeholder=placeholder currentFilter=currentfilter allItems=command.allRoutes validItems=command.visibleRoutes customPicker=routesCustomPicker; route, isValid>
              <input type="checkbox" name="${status.expression}" value="${route.code}"
                     data-short-value="${route.code?upper_case}" ${filters.contains_by_code(command.routes, route)?string('checked','')} <#if !isValid>disabled</#if>>
              <@fmt.route_name route false />
            </@filters.filter>

            <#assign placeholder = "All attendance" />
            <#assign currentfilter><@filters.current_filter_value "modesOfAttendance" placeholder; moa>${moa.shortName?capitalize}</@filters.current_filter_value></#assign>
            <@filters.filter "modesOfAttendance" "command.modesOfAttendance" placeholder currentfilter command.allModesOfAttendance; moa>
              <input type="checkbox" name="${status.expression}" value="${moa.code}" data-short-value="${moa.shortName?capitalize}"
                      ${filters.contains_by_code(command.modesOfAttendance, moa)?string('checked','')}>
              ${moa.fullName}
            </@filters.filter>

            <#assign placeholder = "All years" />
            <#assign currentfilter><@filters.current_filter_value "yearsOfStudy" placeholder; year>${year}</@filters.current_filter_value></#assign>
            <@filters.filter "yearsOfStudy" "command.yearsOfStudy" placeholder currentfilter command.allYearsOfStudy command.allYearsOfStudy "Year "; yearOfStudy>
              <input type="checkbox" name="${status.expression}" value="${yearOfStudy}" data-short-value="${yearOfStudy}"
                      ${command.yearsOfStudy?seq_contains(yearOfStudy)?string('checked','')}>
              ${yearOfStudy}
            </@filters.filter>

            <#assign placeholder = "All statuses" />
            <#assign currentfilter><@filters.current_filter_value "sprStatuses" placeholder; sprStatus>${sprStatus.shortName?capitalize}</@filters.current_filter_value></#assign>
            <@filters.filter "sprStatuses" "command.sprStatuses" placeholder currentfilter command.allSprStatuses; sprStatus>
              <input type="checkbox" name="${status.expression}" value="${sprStatus.code}"
                     data-short-value="${sprStatus.shortName?capitalize}" ${filters.contains_by_code(command.sprStatuses, sprStatus)?string('checked','')}>
              ${sprStatus.fullName}
            </@filters.filter>
          </div>

          <div class="mitcircs-submission-filters__submission">
            <i class="fal fa-file-alt fa-fw"></i>

            <#assign placeholder = "All affected modules" />
            <#assign modulesCustomPicker>
              <div class="module-search input-append input-group">
                <input class="module-search-query module prevent-reload form-control" type="text" value="" placeholder="Search for a module" />
                <span class="add-on input-group-addon"><i class="icon-search fa fa-search"></i></span>
              </div>
            </#assign>
            <#assign currentfilter><@filters.current_filter_value "affectedAssessmentModules" placeholder; module>${module.code?upper_case}</@filters.current_filter_value></#assign>
            <@filters.filter name="modules" path="command.affectedAssessmentModules" placeholder=placeholder currentFilter=currentfilter allItems=command.allModules customPicker=modulesCustomPicker; module>
              <input type="checkbox" name="${status.expression}"
                     value="${module.code}"
                     data-short-value="${module.code?upper_case}"
                      ${filters.contains_by_code(command.affectedAssessmentModules, module)?string('checked','')}>
              <@fmt.module_name module false />
            </@filters.filter>

            <#assign placeholder = "All states" />
            <#assign currentfilter><@filters.current_filter_value "state" placeholder; state>${state.entryName}</@filters.current_filter_value></#assign>
            <@filters.filter "state" "command.state" placeholder currentfilter allSubmissionStates; state>
              <input type="checkbox" name="${status.expression}" value="${state.entryName}" data-short-value="${state.entryName}"
                      ${filters.contains_by_enum(command.state, state)?string('checked','')}>
              ${state.entryName}
            </@filters.filter>

            <br>
            <@bs3form.labelled_form_group path="includesStartDate" labelText="Dates from">
              <div class="input-group">
                <@f.input type="text" path="includesStartDate" cssClass="form-control date-picker input-sm" autocomplete="off" />
                <span class="input-group-addon"><i class="fa fa-calendar"></i></span>
              </div>
            </@bs3form.labelled_form_group>

            <@bs3form.labelled_form_group path="includesEndDate" labelText="to">
              <div class="input-group">
                <@f.input type="text" path="includesEndDate" cssClass="form-control date-picker input-sm" autocomplete="off" />
                <span class="input-group-addon"><i class="fa fa-calendar"></i></span>
              </div>
            </@bs3form.labelled_form_group>

            <br>
            <@bs3form.labelled_form_group path="approvedStartDate" labelText="Submitted between">
              <div class="input-group">
                <@f.input type="text" path="approvedStartDate" cssClass="form-control date-picker input-sm" autocomplete="off" />
                <span class="input-group-addon"><i class="fa fa-calendar"></i></span>
              </div>
            </@bs3form.labelled_form_group>

            <@bs3form.labelled_form_group path="approvedEndDate" labelText="and">
              <div class="input-group">
                <@f.input type="text" path="approvedEndDate" cssClass="form-control date-picker input-sm" autocomplete="off" />
                <span class="input-group-addon"><i class="fa fa-calendar"></i></span>
              </div>
            </@bs3form.labelled_form_group>
          </div>

          <button type="button" class="clear-all-filters btn btn-sm btn-filter">
            Clear filters
          </button>
        </@f.form>
      </div>
    </div>

    <div class="filter-results mitcircs-submission-list">
      <i class="fal fa-spinner fa-spin"></i> Loading&hellip;
    </div>
  </div>
</#escape>