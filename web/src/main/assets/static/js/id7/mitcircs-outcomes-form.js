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
      .find('.mitcircs-form__fields__section__nested-checkboxes')
      .each((i, container) => {
        const $container = $(container);
        const $target = $($(container).data('target'));
        const $targetValue = $(container).data('target-value');

        $target.on('input change', () => {
          $container.collapse( $target.filter(':checked').val() === $targetValue ? 'show' : 'hide');
        }).trigger('change');
      });

    $form
      .find('.mitcircs-form__fields__section__optional-question')
      .each((i, container) => {
        const $container = $(container);
        const $target = $(':input[name=outcomeGrading]');
        $target.on('input change', () => {
          const enabled = $target.filter(':checked').val() !== 'Rejected';
          $container.collapse(enabled ? 'show' : 'hide');
        }).trigger('change');
      });

  }

}

function init() {
  $('.mitcircs-outcomes-form').each((i, el) => {
    $(el).data('tabula.mitCircsOutcomesForm', new MitCircsOutcomesForm(el));
  });
}

$(() => init());