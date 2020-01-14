<h1>User settings for ${user.firstName}</h1>

<#if success!false>
  <div class="alert alert-info">Your preferred settings have been updated.</div>
</#if>
<@f.form method="post" action="${url('/settings')}" modelAttribute="userSettingsCommand">

  <#if isCourseworkModuleManager || isDepartmentalAdmin>
    <@bs3form.labelled_form_group path="alertsSubmission" labelText="Submission email alerts">
      <@bs3form.radio>
        <@f.radiobutton path="alertsSubmission" value="allSubmissions" />
        All submissions
      </@bs3form.radio>
      <@bs3form.radio>
        <@f.radiobutton path="alertsSubmission" value="lateSubmissions" />
        Noteworthy submissions (late, late within extension, suspected plagiarism)
      </@bs3form.radio>
      <@bs3form.radio>
        <@f.radiobutton path="alertsSubmission" value="none" />
        No alerts
      </@bs3form.radio>
    </@bs3form.labelled_form_group>

    <@bs3form.labelled_form_group path="newAssignmentSettings" labelText="New assignment settings">
      <@bs3form.radio>
        <@f.radiobutton path="newAssignmentSettings" value="restoreRecent" />
        Automatically copy details from the assignment created most recently in the same department
      </@bs3form.radio>
      <@bs3form.radio>
        <@f.radiobutton path="newAssignmentSettings" value="none" />
        Do not automatically copy anything
      </@bs3form.radio>
    </@bs3form.labelled_form_group>
  </#if>

  <@bs3form.labelled_form_group path="weekNumberingSystem" labelText="Week numbering system">
    <@bs3form.radio>
      <@f.radiobutton path="weekNumberingSystem" value="" />
      Use the department's choice of week numbering system
    </@bs3form.radio>
    <@bs3form.radio>
      <@f.radiobutton path="weekNumberingSystem" value="term" />
      Count weeks from 1-10 for each term (the first week of the Spring term is Term 2, week 1)
    </@bs3form.radio>
    <@bs3form.radio>
      <@f.radiobutton path="weekNumberingSystem" value="cumulative" />
      Count term weeks cumulatively (the first week of the Spring term is Term 2, week 11)
    </@bs3form.radio>
    <@bs3form.radio>
      <@f.radiobutton path="weekNumberingSystem" value="academic" />
      Use academic week numbers, including vacations (the first week of the Spring term is week 15)
    </@bs3form.radio>
    <@bs3form.radio>
      <@f.radiobutton path="weekNumberingSystem" value="none" />
      Use no week numbers, displaying dates instead
    </@bs3form.radio>
  </@bs3form.labelled_form_group>

  <@bs3form.labelled_form_group path="bulkEmailSeparator" labelText="Bulk email separator">
    <@bs3form.radio>
      <@f.radiobutton path="bulkEmailSeparator" value=";" />
      Semi-colon (Microsoft Outlook)
    </@bs3form.radio>
    <@bs3form.radio>
      <@f.radiobutton path="bulkEmailSeparator" value="," />
      Comma (Thunderbird, Gmail, Apple Mail, Evolution)
    </@bs3form.radio>
  </@bs3form.labelled_form_group>

  <fieldset>
    <h2>Notifications</h2>

    <#if features.notificationBatching>
      <@bs3form.labelled_form_group path="batchedNotifications" labelText="Batching email notifications" help="By batching email notifications, you can reduce the number of emails you're getting from Tabula. Instead of sending emails immediately, we'll group emails of the same type together and send you a single summary email.">
        <#list batchedNotificationSettings as setting>
          <@bs3form.radio>
            <@f.radiobutton path="batchedNotifications" value=setting.entryName />
            ${setting.description}
          </@bs3form.radio>
        </#list>
      </@bs3form.labelled_form_group>
    </#if>

    <@bs3form.labelled_form_group>
      <p>Send me notifications about:</p>

      <@bs3form.checkbox>
        <@f.checkbox path="smallGroupEventAttendanceReminderEnabled" />
        Small group attendance
      </@bs3form.checkbox>

      <@bs3form.checkbox>
        <@f.checkbox path="finaliseFeedbackNotificationEnabled" />
        Coursework submissions which have been marked
      </@bs3form.checkbox>
      <#if isDepartmentalAdmin>
        <@bs3form.checkbox>
          <@f.checkbox path="deptAdminReceiveStudentComments" />
          Student requests for assistance from a Departmental Administrator
        </@bs3form.checkbox>

        <#list deptsWithNoOtherContacts as item>
          <div class="alert alert-warning" role="alert">
            <strong>Important:</strong> You're the last departmental contact in ${item}.
            If you opt out, students will be unable to request departmental assistance from within Tabula.
          </div>
        </#list>
      </#if>
    </@bs3form.labelled_form_group>
  </fieldset>

  <@bs3form.labelled_form_group>
    <input type="submit" value="Save" class="btn btn-primary">
    <a class="btn btn-default" href="${url('/')}">Cancel</a>
  </@bs3form.labelled_form_group>

</@f.form>
