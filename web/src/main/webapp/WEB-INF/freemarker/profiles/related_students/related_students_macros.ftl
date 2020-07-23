<#escape x as x?html>

  <#macro row student studentCourseDetails="" showMeetings=false meetingInfoPair="" showSelectStudents=false>
    <#if studentCourseDetails?has_content>
      <#assign courseDetails=studentCourseDetails.urlSafeId>
    <#else>
      <#assign courseDetails="">
    </#if>

    <tr class="related_student">
      <#if showSelectStudents>
        <td><input class="collection-checkbox" type="checkbox" name="students" data-fullname="${student.fullName}" value="${student.universityId}"
                   data-student-course-details="${courseDetails}" /></td>
      </#if>
      <td>
        <@fmt.member_photo student "tinythumbnail" />
      </td>
      <td>${student.firstName}</td>
      <td>${student.lastName}</td>
      <#if studentCourseDetails?has_content>
        <td><a class="profile-link" href="/profiles/view/course/${studentCourseDetails.urlSafeId}">${studentCourseDetails.student.universityId}</a></td>
      <#else>
        <td><a class="profile-link" href="<@routes.profiles.profile student />">${student.universityId}</a></td>
      </#if>
      <td>${student.groupName!""}</td>
      <#if studentCourseDetails?has_content>
        <td>${(studentCourseDetails.latestStudentCourseYearDetails.yearOfStudy)!""}</td>
        <td>${(studentCourseDetails.currentRoute.name)!""}</td>
      <#else>
        <td>${(student.mostSignificantCourseDetails.latestStudentCourseYearDetails.yearOfStudy)!""}</td>
        <td>${(student.mostSignificantCourseDetails.currentRoute.name)!""}</td>
      </#if>
      <#if showMeetings>
        <#if meetingInfoPair._1()?has_content>
          <#assign lastMeeting = meetingInfoPair._1() />
          <#assign pendingApprovals = meetingInfoPair._2() />
          <td data-datesort="${(lastMeeting.meetingDate.millis?c)!''}">
            <@fmt.date date=lastMeeting.meetingDate shortMonth=true includeTime=true /><br />
            <span tabindex="0" class="badge progress-bar-<#if (pendingApprovals > 2)>danger<#elseif (pendingApprovals > 0)>warning<#else>success</#if>  use-tooltip"
                  data-original-title="Number of unapproved meetings">${pendingApprovals}</span>
          </td>
        <#else>
          <td></td>
        </#if>
      </#if>
    </tr>
  </#macro>

  <#macro tableWithMeetingsColumn items meetingsMap showSelectStudents=false>
    <@table items=items showMeetings=true meetingsMap=meetingsMap showSelectStudents=showSelectStudents/>
  </#macro>
<#-- Print out a table of students/agents.-->
  <#macro table items showMeetings=false meetingsMap="" showSelectStudents=false>
    <table class="related_students student-list table table-striped table-condensed">
      <thead>
      <tr>
      <tr>
        <#if showSelectStudents>
          <th class="check-col no-sort"><input type="checkbox" class="collection-check-all use-tooltip" title="Select/unselect all"></th>
        </#if>
        <th class="photo-col no-sort">Photo</th>
        <th class="student-col">First name</th>
        <th class="student-col">Last name</th>
        <th class="id-col">ID</th>
        <th class="type-col">Type</th>
        <th class="year-col">Year</th>
        <th class="course-but-photo-col">Course</th>
        <#if showMeetings>
          <th class="meetings-col">Last met</th></#if>
      </tr>
      </thead>
      <tbody>
      <#list items as item>
        <#if item.scjCode??>
          <@row item.student item showMeetings=showMeetings meetingsMap[item.student.universityId] showSelectStudents=showSelectStudents/>
        <#else>
          <@row item />
        </#if>

      </#list>
      </tbody>
    </table>

    <#if !student_table_script_included??>
      <script type="text/javascript" nonce="${nonce()}">
        (function ($) {
          $('.related_students').tablesorter({
            sortLocaleCompare: true,
            textAttribute: 'data-sortby',
          }).on('tablesorter-ready', (e) => {
            const $table = $(e.target);
            $('.student-list').bigList({});
            /*
             * Beware: performance is garbage if you use data-dynamic-sort="true" because it will re-init
             * the whole thing every time there's a change. Probably don't use it until we've found some
             * way to optimise the amount of time it takes tablesorter to init.
             */
            if ($table.data('dynamic-sort') && !$table.data('dynamic-sort-initialised')) {
              $table.data('dynamic-sort-initialised', true);

              $table.on('change', () => $table.trigger('update'));
            }
          });
        })(jQuery);
      </script>
    <#-- deliberately a global assign, so only the first macro includes script. -->
      <#assign student_table_script_included=true />
    </#if>
  </#macro>

  <#macro myStudents viewerRelationshipTypes smallGroups="">
    <section id="relationship-details" class="clearfix">
      <h4>My students</h4>
      <ul>
        <#list viewerRelationshipTypes as relationshipType>
          <li><h5><a id="relationship-${relationshipType.urlPart}"
                     href="<@routes.profiles.relationship_students relationshipType />">${relationshipType.studentRole?cap_first}s</a></h5></li>
        </#list>
        <#if smallGroups?has_content>
          <#list smallGroups as smallGroup>
            <#assign _groupSet=smallGroup.groupSet />
            <#assign _module=smallGroup.groupSet.module />
            <li><a href="<@routes.profiles.smallgroup smallGroup />">
                ${_module.code?upper_case} (${_module.name}) ${_groupSet.name}, ${smallGroup.name}
              </a></li>
          </#list>
        </#if>
      </ul>
    </section>


  </#macro>

</#escape>
