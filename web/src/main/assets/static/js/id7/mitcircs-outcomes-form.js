/* eslint-env browser */
import $ from 'jquery';

class MitCircsOutcomesForm {

  constructor(form) {
    this.form = form;
    this.init();
  }

  init() {
    const {form} = this;
    const $form = $(form);

    $form
      .find('.mitcircs-outcomes-form__nested-checkboxes')
      .each((i, container) => {
        const $container = $(container);
        const $target = $($(container).data('target'));
        $target.on('input change', () => {
          $container.collapse($target.is(":checked") ? 'show' : 'hide');
        }).trigger('change');
      })
  }

}

function init() {
  $('.mitcircs-outcomes-form').each((i, el) => {
    $(el).data('tabula.mitCircsOutcomesForm', new MitCircsOutcomesForm(el));
  });
}

$(() => init());