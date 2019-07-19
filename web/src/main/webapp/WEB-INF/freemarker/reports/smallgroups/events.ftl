<#escape x as x?html>
  <#import "../reports_macros.ftl" as reports_macros />

  <h1>All events</h1>

  <#assign reportUrl><@routes.reports.events department academicYear /></#assign>
  <@reports_macros.reportLoader reportUrl=reportUrl hasDatePicker=false>
    <ul class="dropdown-menu">
      <li><a href="#" data-href="<@routes.reports.eventsDownloadCsv department academicYear />">CSV</a></li>
      <li><a href="#" data-href="<@routes.reports.eventsDownloadXlsx department academicYear />">Excel</a></li>
      <li><a href="#" data-href="<@routes.reports.eventsDownloadXml department academicYear />">XML</a></li>
    </ul>
  </@reports_macros.reportLoader>


  <script nonce="${nonce()}">
    jQuery(function ($) {
      if (window.ReportBuilder == undefined)
        return false;

      window.ReportBuilder.rowKey = 'events';

      window.ReportBuilder.buildHeader = function () {
        var container = $('<tr/>');
        container.append(
          $('<th/>').addClass('sortable').text('Department name')
        ).append(
          $('<th/>').addClass('sortable').text('Event name')
        ).append(
          $('<th/>').addClass('sortable').text('Module title')
        ).append(
          $('<th/>').addClass('sortable').text('Day')
        ).append(
          $('<th/>').addClass('sortable').text('Start')
        ).append(
          $('<th/>').addClass('sortable').text('Finish')
        ).append(
          $('<th/>').addClass('sortable').text('Location')
        ).append(
          $('<th/>').addClass('sortable').text('Size')
        ).append(
          $('<th/>').addClass('sortable').text('Weeks')
        ).append(
          $('<th/>').addClass('sortable').text('Staff')
        );
        return container;
      };

      window.ReportBuilder.buildRow = function (data) {
        var container = $('<tr/>');
        var dayAsNumber = 0;
        switch (data.day) {
          case 'Monday':
            dayAsNumber = 1;
            break;
          case 'Tuesday':
            dayAsNumber = 2;
            break;
          case 'Wednesday':
            dayAsNumber = 3;
            break;
          case 'Thursday':
            dayAsNumber = 4;
            break;
          case 'Friday':
            dayAsNumber = 5;
            break;
          case 'Saturday':
            dayAsNumber = 6;
            break;
          case 'Sunday':
            dayAsNumber = 7;
            break;
        }
        container.append(
          $('<td/>').text(data.departmentName)
        ).append(
          $('<td/>').text(data.eventName)
        ).append(
          $('<td/>').text(data.moduleTitle)
        ).append(
          $('<td/>').data('sortby', dayAsNumber).text(data.day)
        ).append(
          $('<td/>').text(data.start)
        ).append(
          $('<td/>').text(data.finish)
        ).append(
          $('<td/>').text(data.location)
        ).append(
          $('<td/>').text(data.size)
        ).append(
          $('<td/>').text(data.weeks)
        ).append(
          $('<td/>').text(data.staff)
        );
        return container;
      };

    });
  </script>

</#escape>