<#import "*/cm2_macros.ftl" as cm2 />
<#escape x as x?html>

  <@cm2.assignmentHeader "Submission receipt" assignment "for" />

<#if submission??>
<#if sent>

<p>A fresh copy of the receipt email has been sent to <strong>${user.email}</strong>. If it doesn't appear in your inbox after
  a few minutes, check your spam folder.</p>

<#elseif hasEmail>

<p>The receipt email could not be sent to <strong>${user.email}</strong>. Please try again later.</p>

<#else>

<p>The receipt email could not be sent because you do not have a registered email address. Please contact the
<a target="_blank" href="https://warwick.ac.uk/helpdesk">IT Services Help Desk</a> for assistance.</p>

</#if>
<#else>

<p>You haven't submitted this assignment.</p>

</#if>

<p><a href="<@routes.cm2.assignment assignment=assignment />">Back to assignment page</a></p>
</#escape>