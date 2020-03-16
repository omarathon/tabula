/* eslint-env browser */

import $ from 'jquery';
import moment from 'moment-timezone';

const LocalTimesPluginDefaultConfig = {
  serverTimezone: 'Europe/London',
  serverTimezoneString: 'UK time',
  cacheTimezoneDetection: true,
  target: 'time[datetime].datetime-with-local',
  shortStyle: '.datetime-with-local--short',
  extendedStyle: '.datetime-with-local--extended',
  defaultStyle: 'short',
};

class LocalTimesPlugin {
  constructor(options) {
    this.options = $.extend({}, LocalTimesPluginDefaultConfig, options);

    // Quick exit if the user's timezone matches the server timezone and we're caching the result
    if (
      this.options.cacheTimezoneDetection
      && this.userTimezone() === this.options.serverTimezone
    ) {
      return;
    }

    this.addLocalTimes();

    // Set up a MutationObserver
    if (typeof MutationObserver !== 'undefined' && this.options.container.length) {
      const observer = new MutationObserver((objects) => {
        $.each(objects, (i, mutationRecord) => {
          if (mutationRecord.type === 'childList' && mutationRecord.addedNodes.length > 0) {
            this.addLocalTimes(mutationRecord.target);
          }
        });
      });

      this.options.container.each((i, container) => observer.observe(container, {
        childList: true,
        subtree: true,
      }));
    } else {
      // Just run once (more) on DOM ready
      $(() => this.addLocalTimes());
    }
  }

  userTimezone() {
    return moment.tz.guess(!this.options.cacheTimezoneDetection);
  }

  addLocalTimes(target) {
    $(target || this.options.container)
      .find(this.options.target)
      .filter((i, el) => $(el).data('local-times-plugin.initialised') !== true)
      .each((i, el) => {
        const $time = $(el);
        const tz = this.userTimezone();
        const isDifferentTz = this.userTimezone() !== this.options.serverTimezone;

        if (isDifferentTz) {
          let style = this.options.defaultStyle;
          if ($time.is(this.options.shortStyle)) style = 'short';
          else if ($time.is(this.options.extendedStyle)) style = 'extended';

          const serverMoment = moment.tz($time.attr('datetime'), this.options.serverTimezone);
          const localMoment = serverMoment.clone().tz(tz);

          const formattedLocalTime = localMoment.format('HH:mm');

          let formatted;
          // .isSame(server, 'day') doesn't work here, which is troubling
          if (localMoment.format('YYYY-MM-DD') === serverMoment.format('YYYY-MM-DD')) {
            formatted = formattedLocalTime;
          } else {
            let formattedLocalDate;
            if ($time.data('relative') === 'true') {
              const localTodayMoment = moment.tz();
              const localYesterdayMoment = localTodayMoment.clone().subtract(1, 'day');
              const localTomorrowMoment = localTodayMoment.clone().add(1, 'day');

              if (localMoment.isSame(localTodayMoment, 'day')) {
                formattedLocalDate = 'Today';
              } else if (localMoment.isSame(localYesterdayMoment, 'day')) {
                formattedLocalDate = 'Yesterday';
              } else if (localMoment.isSame(localTomorrowMoment, 'day')) {
                formattedLocalDate = 'Tomorrow';
              }
            }

            if (typeof formattedLocalDate === 'undefined') {
              // Mon 29ᵗʰ April
              formattedLocalDate = localMoment.format('ddd Do MMMM');
            }

            // Don't show the date if it's the same
            formatted = `${formattedLocalTime}&#8194;${formattedLocalDate}`;
          }

          if (style === 'extended') {
            $time.append(` ${this.options.serverTimezoneString}, which is ${formatted} ${tz} time`);
          } else if (style === 'short') {
            const $icon = $('<i />')
              .addClass('fad fa-user-clock')
              .append(
                $('<span />')
                  .addClass('sr-only')
                  .text('Convert to your local time'),
              );

            $icon.tabulaPopover({
              trigger: 'click',
              container: 'body',
              html: true,
              content: `${formatted} ${tz} time`,
              placement: 'right',
            });

            $time.append(` ${this.options.serverTimezoneString} `).append($icon);
          } else throw new Error(`Unexpected style ${style}`);
        }

        if (isDifferentTz || this.options.cacheTimezoneDetection) {
          $time.data('local-times-plugin.initialised', true);
        }
      });
  }
}

$.fn.localTimes = function localTimesPlugin(options) {
  const o = options || {};

  function attach(i, element) {
    const $container = $(element);
    const plugin = new LocalTimesPlugin($.extend({}, $container.data(), o, {
      container: $container,
    }));

    $container.data('local-times-plugin', plugin);
  }

  return this.each(attach);
};

// No need to wait for DOMReady, we're using MutationObserver
$(document).localTimes();
