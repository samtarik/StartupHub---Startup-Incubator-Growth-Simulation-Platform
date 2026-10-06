/* Chart.js helpers.

   Charts read their colours from the CSS custom properties, and every chart is
   registered so it can be rebuilt when the theme changes - that is what keeps
   dark mode from leaving unreadable axis labels behind. */

const Charts = {
  registry: new Map(),

  token(name, fallback) {
    const v = getComputedStyle(document.documentElement).getPropertyValue(name).trim();
    return v || fallback;
  },

  palette() {
    return {
      brand: this.token('--brand', '#3E5CFF'),
      growth: this.token('--growth', '#12B76A'),
      burn: this.token('--burn', '#F79009'),
      danger: this.token('--danger', '#E5484D'),
      ink: this.token('--ink', '#14161F'),
      muted: this.token('--ink-muted', '#5B6178'),
      grid: this.token('--chart-grid', 'rgba(20,22,31,0.08)'),
      surface: this.token('--surface', '#FFFFFF'),
    };
  },

  baseOptions(extra = {}) {
    const c = this.palette();
    return Object.assign({
      responsive: true,
      maintainAspectRatio: false,
      interaction: { intersect: false, mode: 'index' },
      plugins: {
        legend: {
          labels: { color: c.muted, usePointStyle: true, boxWidth: 8, padding: 16,
                    font: { family: 'Inter, sans-serif', size: 12 } },
        },
        tooltip: {
          backgroundColor: c.ink, padding: 10, cornerRadius: 8, displayColors: true,
          titleFont: { family: 'Inter, sans-serif' }, bodyFont: { family: 'IBM Plex Mono, monospace' },
        },
      },
      scales: {
        x: { grid: { display: false }, ticks: { color: c.muted, font: { family: 'Inter, sans-serif', size: 11 } } },
        y: {
          beginAtZero: true,
          grid: { color: c.grid, drawBorder: false },
          ticks: {
            color: c.muted, font: { family: 'IBM Plex Mono, monospace', size: 11 },
            callback: (v) => Fmt.money(v),
          },
        },
      },
    }, extra);
  },

  /* Builds (or rebuilds) a chart and remembers how, so the theme switch can
     re-run the same builder against the new colours. */
  render(canvasId, builder) {
    const canvas = document.getElementById(canvasId);
    if (!canvas || typeof Chart === 'undefined') return null;

    const existing = this.registry.get(canvasId);
    if (existing && existing.chart) existing.chart.destroy();

    const chart = new Chart(canvas.getContext('2d'), builder(this.palette(), this));
    this.registry.set(canvasId, { chart, builder });
    return chart;
  },

  destroy(canvasId) {
    const existing = this.registry.get(canvasId);
    if (existing && existing.chart) existing.chart.destroy();
    this.registry.delete(canvasId);
  },

  destroyAll() {
    this.registry.forEach(entry => entry.chart && entry.chart.destroy());
    this.registry.clear();
  },

  repaintAll() {
    const entries = Array.from(this.registry.entries());
    entries.forEach(([id, entry]) => this.render(id, entry.builder));
  },

  /* ---- Ready-made chart types ------------------------------------------- */
  revenueVsExpense(canvasId, labels, revenue, expense) {
    return this.render(canvasId, (c, self) => ({
      type: 'line',
      data: {
        labels,
        datasets: [
          { label: 'Revenue', data: revenue, borderColor: c.growth, backgroundColor: c.growth + '22',
            tension: 0.32, fill: true, borderWidth: 2, pointRadius: 3, pointBackgroundColor: c.growth },
          { label: 'Expenses', data: expense, borderColor: c.burn, backgroundColor: c.burn + '1A',
            tension: 0.32, fill: true, borderWidth: 2, pointRadius: 3, pointBackgroundColor: c.burn },
        ],
      },
      options: self.baseOptions(),
    }));
  },

  investmentMix(canvasId, labels, values) {
    return this.render(canvasId, (c, self) => ({
      type: 'doughnut',
      data: {
        labels,
        datasets: [{
          data: values,
          backgroundColor: [c.brand, c.growth, c.burn, c.danger],
          borderColor: c.surface,
          borderWidth: 3,
        }],
      },
      options: {
        responsive: true, maintainAspectRatio: false, cutout: '62%',
        plugins: {
          legend: { position: 'bottom',
            labels: { color: c.muted, usePointStyle: true, boxWidth: 8, padding: 14,
                      font: { family: 'Inter, sans-serif', size: 12 } } },
          tooltip: {
            backgroundColor: c.ink, padding: 10, cornerRadius: 8,
            callbacks: { label: (ctx) => ` ${ctx.label}: ${Fmt.money(ctx.parsed, { compact: false })}` },
          },
        },
      },
    }));
  },

  campaignPerformance(canvasId, labels, budgets, conversions) {
    return this.render(canvasId, (c, self) => ({
      type: 'bar',
      data: {
        labels,
        datasets: [
          { label: 'Budget', data: budgets, backgroundColor: c.brand, borderRadius: 6,
            maxBarThickness: 34, yAxisID: 'y' },
          { label: 'Conversions', data: conversions, backgroundColor: c.growth, borderRadius: 6,
            maxBarThickness: 34, yAxisID: 'y1' },
        ],
      },
      options: self.baseOptions({
        scales: {
          x: { grid: { display: false }, ticks: { color: c.muted, font: { family: 'Inter, sans-serif', size: 11 } } },
          y: { beginAtZero: true, position: 'left', grid: { color: c.grid, drawBorder: false },
               ticks: { color: c.muted, font: { family: 'IBM Plex Mono, monospace', size: 11 },
                        callback: (v) => Fmt.money(v) } },
          y1: { beginAtZero: true, position: 'right', grid: { display: false },
                ticks: { color: c.muted, font: { family: 'IBM Plex Mono, monospace', size: 11 } } },
        },
      }),
    }));
  },

  expenseBreakdown(canvasId, labels, values) {
    return this.render(canvasId, (c, self) => ({
      type: 'bar',
      data: { labels, datasets: [{ label: 'Spend', data: values, backgroundColor: c.burn, borderRadius: 6, maxBarThickness: 26 }] },
      options: self.baseOptions({
        indexAxis: 'y',
        plugins: { legend: { display: false },
                   tooltip: { backgroundColor: c.ink, padding: 10, cornerRadius: 8 } },
        scales: {
          x: { beginAtZero: true, grid: { color: c.grid, drawBorder: false },
               ticks: { color: c.muted, font: { family: 'IBM Plex Mono, monospace', size: 11 },
                        callback: (v) => Fmt.money(v) } },
          y: { grid: { display: false }, ticks: { color: c.muted, font: { family: 'Inter, sans-serif', size: 11 } } },
        },
      }),
    }));
  },
};

/* Rebuild every chart when the user flips the theme. */
document.addEventListener('themechange', () => Charts.repaintAll());
