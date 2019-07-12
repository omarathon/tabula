<#if exception?? && (((features.renderStackTracesForAllUsers)!false) || ((user.sysadmin)!false))>
  <p>
    <button type="button" class="btn btn-danger" data-toggle="collapse" data-target="#dev">
      <span class="tabula-tooltip" data-title="This information is only available to sysadmins"><i class="fal fa-user-crown"></i></span>
      Show technical details about this error
    </button>
  </p>

  <pre id="dev" class="collapse" style="overflow-x:scroll;">${stackTrace}</pre>
</#if>