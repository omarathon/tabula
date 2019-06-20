<#escape x as x?html>
  <#function route_function dept>
    <#local result><@routes.mitcircs.dummyDataGeneration dept /></#local>
    <#return result />
  </#function>
  <@fmt.id7_deptheader "Generate dummy data" route_function "for" />

  <div class="alert alert-info">
    <h2>DANGER WILL ROBINSON</h2>

    <p>This will fill the database with a bunch of dummy data. If you run it repeatedly, it will add yet more data.</p>

    <p>Don't run this on production, ever, and be aware that it's going to be really hard to undo without junking the database and
      starting again.</p>
  </div>

  <@f.form method="POST" modelAttribute="command" class="double-submit-protection">
    <p>
      <button type="submit" class="btn btn-danger">Generate dummy data</button>
    </p>
  </@f.form>
</#escape>