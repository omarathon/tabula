<h1>Masquerade as a different user</h1>

<#if !user.sysadmin && user.masquerader>
<p>You are not a system admin but are in a group of people able to masquerade freely as another user.</p>
</#if>

<#if user.masquerading>

<p>Masquerading as ${user.apparentId} (${user.apparentUser.fullName}).</p>

<@f.form method="post" action="/admin/masquerade">
  <input type="hidden" name="action" value="remove" />
  <input type="submit" value="Unmask">
</@f.form>

<#else>

<#if actionMessage?default('') = "removed">
<p>You are no longer masquerading.</p>
</#if>

<div>
<@f.form method="post" action="/admin/masquerade" command="">
  User ID: <@form.userpicker name="usercode" />
  <input type="submit" value="Mask">
</@f.form>
</div>

</#if>