/*
 * Скрипт главной (подключается в конце home.ejs, defer):
 *  — hero-слайдер: crossfade, autoplay ~5с, свайп, буллеты-прогресс,
 *    каскад появления текста слайда, пауза по hover и при скрытой вкладке.
 * Один баннер → без автоплея. Уважает prefers-reduced-motion (без autoplay).
 */
(function () {
  'use strict';

  var reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  var root = document.querySelector('[data-hero]');
  if (!root) return;

  var slides = Array.prototype.slice.call(root.querySelectorAll('[data-hero-slide]'));
  if (slides.length === 0) return;

  var DELAY = 6000;
  var current = 0;
  var timer = null;

  /* Каскад появления [data-hero-anim] активного слайда */
  function animateContent(slide) {
    var items = slide.querySelectorAll('[data-hero-anim]');
    items.forEach(function (el, i) {
      el.style.setProperty('--d', i * 0.12 + 0.1 + 's');
    });
  }

  /* Буллеты */
  var bulletsWrap = root.querySelector('[data-hero-bullets]');
  var bullets = [];
  if (bulletsWrap && slides.length > 1) {
    bulletsWrap.style.setProperty('--hero-delay', DELAY + 'ms');
    slides.forEach(function (_, i) {
      var b = document.createElement('button');
      b.className = 'hero__bullet';
      b.type = 'button';
      var slideLabel = (window.__I18N && window.__I18N.slide) || 'Slide';
      b.setAttribute('aria-label', slideLabel + ' ' + (i + 1));
      b.innerHTML = '<span></span>';
      b.addEventListener('click', function () {
        go(i, true);
      });
      bulletsWrap.appendChild(b);
      bullets.push(b);
    });
  }

  function go(index, userAction) {
    current = (index + slides.length) % slides.length;
    slides.forEach(function (s, i) {
      s.classList.toggle('is-active', i === current);
    });
    bullets.forEach(function (b, i) {
      // перезапуск CSS-анимации заливки буллета
      b.classList.remove('is-active');
      if (i === current) {
        void b.offsetWidth;
        b.classList.add('is-active');
      }
    });
    animateContent(slides[current]);
    if (userAction) restart();
  }

  function next() {
    go(current + 1);
  }
  function prev() {
    go(current - 1);
  }

  function start() {
    if (reduceMotion || slides.length < 2) return;
    stop();
    timer = window.setInterval(next, DELAY);
  }
  function stop() {
    if (timer) {
      window.clearInterval(timer);
      timer = null;
    }
  }
  function restart() {
    stop();
    start();
  }

  /* Стрелки */
  var prevBtn = root.querySelector('[data-hero-prev]');
  var nextBtn = root.querySelector('[data-hero-next]');
  if (prevBtn) prevBtn.addEventListener('click', function () { go(current - 1, true); });
  if (nextBtn) nextBtn.addEventListener('click', function () { go(current + 1, true); });

  /* Пауза по hover */
  root.addEventListener('mouseenter', stop);
  root.addEventListener('mouseleave', start);

  /* Пауза при скрытой вкладке */
  document.addEventListener('visibilitychange', function () {
    if (document.hidden) stop();
    else start();
  });

  /* Свайп (pointer) */
  var startX = null;
  root.addEventListener('pointerdown', function (e) {
    startX = e.clientX;
  });
  root.addEventListener('pointerup', function (e) {
    if (startX === null) return;
    var dx = e.clientX - startX;
    if (Math.abs(dx) > 45) {
      if (dx < 0) go(current + 1, true);
      else go(current - 1, true);
    }
    startX = null;
  });

  /* Инициализация */
  go(0);
  start();
})();
