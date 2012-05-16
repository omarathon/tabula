<h1>Evaluator</h1>
<p>Available variables: <code>session</code>, <code>beanFactory</code>.</p>
<@f.form method="post" action="/sysadmin/repl">
<textarea name="query" class="span6">
${query!""}
</textarea>
<br>
<input type="submit" class="btn" value="Evaluate">
</@f.form>

<#if response.value??>
<#assign stringValue=response.stringValue/>
<#elseif response.isNone>
<#assign stringValue=None/>
</#if>

<#if stringValue??>
<pre class="well"><code>
${stringValue}: ${response.value.class.simpleName}
</code></pre>
</#if>
<#if response.exception??>
<p class="alert alert-warning">
<h3>${response.exception.class.simpleName}</h3>
${response.exception.message}
</p>
</#if>