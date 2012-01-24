<#assign fmt=JspTaglibs["/WEB-INF/tld/fmt.tld"]>

<h2>The time</h2>

<p>
  ${timeWelcome} <@fmt.formatDate value=time type="time" />
</p>