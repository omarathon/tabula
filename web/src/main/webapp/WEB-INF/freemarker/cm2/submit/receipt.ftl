<#import "*/cm2_macros.ftl" as cm2 />
<#escape x as x?html>

  <@cm2.assignmentHeader "Submission receipt" assignment "for" />

<#if submission??>
<#if sent>

<p>I've sent a fresh copy of the email to <strong>${user.email}</strong>. If it doesn't show up in your inbox after
  a couple of minutes, check your spam folder in case it has been put in there.</p>

<#elseif hasEmail>

<p>I was unable to send an email to <strong>${user.email}</strong>. Please try again later.</p>

<#else>

<p>I was unable to send a receipt email because you don't have any email address registered. Please contact the
ITS helpdesk who will help you </p>

</#if>
<#else>

<p>You haven't submitted to this assignment.</p>

</#if>

<p><a href="<@routes.cm2.assignment assignment=assignment />">Back to assignment page</a></p>
</#escape>