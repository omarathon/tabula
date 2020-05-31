/* eslint-env browser */
import $ from 'jquery';
import Chart from 'chart.js';
import ScalingAlgorithm from './scaling-algorithm';

$(() => {
  function computeNewMarks(marks) {
    const passMark = $('#passMark').val() * 1;
    const passMarkAdjustment = $('#passMarkAdjustment').val() * 1;
    const upperClassAdjustment = $('#upperClassAdjustment').val() * 1;
    return marks.map(m => ({
      x: m,
      y: ScalingAlgorithm.doScaleMark(m, passMark, passMarkAdjustment, upperClassAdjustment),
    }));
  }

  function scaleLine() {
    const passMark = $('#passMark').val() * 1;
    const passMarkAdjustment = $('#passMarkAdjustment').val() * 1;
    const upperClassAdjustment = $('#upperClassAdjustment').val() * 1;
    return [0, (passMark - passMarkAdjustment), (70 - upperClassAdjustment), 100].map(m => ({
      x: m,
      y: ScalingAlgorithm.doScaleMark(m, passMark, passMarkAdjustment, upperClassAdjustment),
    }));
  }

  if ($('#marks').length !== 0) {
    const markData = JSON.parse($('<textarea/>').html($('#marks').html()).text());
    const marks = Object.values(markData);
    Chart.platform.disableCSSInjection = true;

    const chart = new Chart(document.getElementById('chart').getContext('2d'), {
      type: 'scatter',
      data: {
        datasets: [{
          data: computeNewMarks(marks),
          pointBackgroundColor: '#3e7a73',
          fill: false,
          tension: 0,
        }, {
          fill: false,
          tension: 0,
          pointRadius: 0,
          pointHoverRadius: 0,
          borderWidth: 0,
          showLine: true,
          data: [{
            x: 0,
            y: 0,
          }, {
            x: 100,
            y: 100,
          }],
        }],
      },
      options: {
        legend: false,
        tooltips: false,
        scales: {
          xAxes: [{
            ticks: {
              min: 0,
              max: 100,
            },
            scaleLabel: {
              display: true,
              labelString: 'Raw Mark',
            },
            gridLines: {
              color: '#eee',
              drawOnChartArea: true,
            },
          }],
          yAxes: [{
            ticks: {
              min: 0,
              max: 100,
              padding: 10,
            },
            scaleLabel: {
              display: true,
              labelString: 'Adjusted Mark',
            },
            gridLines: {
              color: '#eee',
              drawOnChartArea: true,
            },
          }],
        },
      },
    });

    $('.form-control').on('change', () => {
      chart.data.datasets[0].data = computeNewMarks(marks);
      chart.data.datasets[1].data = scaleLine();
      chart.update();
    });
  }
});
