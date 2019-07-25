<#import "*/modal_macros.ftlh" as modal />
<#escape x as x?html>
<#-- Pre-save modal for notifications -->
  <@modal.modal id="notify-modal" role="dialog" ariaLabelledby="notify-modal-label">
    <@modal.wrapper>
      <@modal.header>
        <h3 id="notify-modal-label" class="modal-title">Send notifications</h3>
      </@modal.header>

      <@modal.body>
        <p>
          Notify these people via email of this change.
          Notifications will be sent immediately, regardless of when the change is scheduled.
          Note that only students/${relationshipType.agentRole}s whose allocations have been changed will be notified.
        </p>

        <@bs3form.checkbox>
          <input type="checkbox" name="notifyStudent" checked />
          ${relationshipType.studentRole?cap_first}s
        </@bs3form.checkbox>

        <@bs3form.checkbox>
          <input type="checkbox" name="notifyOldAgent" checked />
          Old ${relationshipType.agentRole}s
        </@bs3form.checkbox>

        <@bs3form.checkbox>
          <input type="checkbox" name="notifyNewAgent" checked />
          New ${relationshipType.agentRole}s
        </@bs3form.checkbox>

      </@modal.body>

      <@modal.footer>
        <button type="submit" class="btn btn-primary">Save</button>
        <button type="button" class="btn btn-default" data-dismiss="modal" aria-hidden="true">Cancel</button>
      </@modal.footer>
    </@modal.wrapper>
  </@modal.modal>
</#escape>