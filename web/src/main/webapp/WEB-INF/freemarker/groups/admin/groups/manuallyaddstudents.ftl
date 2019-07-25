<#escape x as x?html>

  <h1>Add students manually</h1>

  <form action="" method="POST" class="mass-add-users">

    <@f.hidden path="findCommand.linkToSits" />
    <@f.hidden path="findCommand.doFind" />
    <@f.hidden path="findCommand.courseTypes" />
    <@f.hidden path="findCommand.routes" />
    <@f.hidden path="findCommand.courses" />
    <@f.hidden path="findCommand.modesOfAttendance" />
    <@f.hidden path="findCommand.yearsOfStudy" />
    <@f.hidden path="findCommand.levelCodes" />
    <@f.hidden path="findCommand.sprStatuses" />
    <@f.hidden path="findCommand.modules" />
    <@f.hidden path="findCommand.hallsOfResidence" />

    <#list findCommand.staticStudentIds as id>
      <input type="hidden" name="staticStudentIds" value="${id}" />
    </#list>
    <#list editMembershipCommand.includedStudentIds as id>
      <input type="hidden" name="includedStudentIds" value="${id}" />
    </#list>
    <#list editMembershipCommand.excludedStudentIds as id>
      <input type="hidden" name="excludedStudentIds" value="${id}" />
    </#list>
    <input type="hidden" name="returnTo" value="${returnTo}">

    <p>Type or paste in a list of usercodes or University IDs here, separated by white space, then click <code>Add</code>.</p>

    <textarea rows="6" class="form-control" name="massAddUsers"></textarea>

    <input
            type="submit"
            class="btn btn-primary spinnable spinner-auto add-students"
            name="${ManageSmallGroupsMappingParameters.manuallyAddSubmit}"
            value="Add"
    />

  </form>

</#escape>