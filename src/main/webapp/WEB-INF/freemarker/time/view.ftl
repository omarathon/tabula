<#assign fmt=JspTaglibs["/WEB-INF/tld/fmt.tld"]>

<h1>The extraordinary time-teller</h1>

<p>
  ${timeWelcome} <@fmt.formatDate value="${time}" type="time" />
</p>