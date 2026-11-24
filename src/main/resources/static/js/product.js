/*
 * Скрипт страницы товара (подключается в конце product.ejs, defer):
 *  — галерея: переключение миниатюр с crossfade главного фото
 *  — zoom по hover на десктопе (transform-origin по позиции курсора)
 * Уважает prefers-reduced-motion (без zoom). Без JS главное фото всё равно видно.
 */
(function () {
  'use strict';

  var reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  var main = document.getElementById('product-main-image');
  if (!main) return;

  var frame = main.closest('.product__main-image');
  var thumbs = Array.prototype.slice.call(document.querySelectorAll('.product__thumb'));

  /* ── Переключение миниатюр с crossfade ────────────────────────────────── */
  thumbs.forEach(function (thumb) {
    thumb.addEventListener('click', function () {
      var full = thumb.dataset.full;
      var alt = thumb.dataset.alt || '';
      if (!full || main.getAttribute('src') === full) return;

      thumbs.forEach(function (t) {
        t.classList.remove('is-active');
      });
      thumb.classList.add('is-active');

      if (reduceMotion || !frame) {
        main.src = full;
        main.alt = alt;
        return;
      }

      frame.classList.add('is-fading');
      var swap = function () {
        main.src = full;
        main.alt = alt;
        main.removeEventListener('transitionend', swap);
      };
      // подстраховка, если transitionend не сработает
      var done = false;
      var apply = function () {
        if (done) return;
        done = true;
        main.src = full;
        main.alt = alt;
        frame.classList.remove('is-fading');
      };
      main.addEventListener('transitionend', apply, { once: true });
      window.setTimeout(apply, 260);
    });
  });

  /* ── Zoom по hover (десктоп) ──────────────────────────────────────────── */
  if (!reduceMotion && frame && window.matchMedia('(hover: hover) and (min-width: 901px)').matches) {
    frame.classList.add('is-zoomable');
    frame.addEventListener('mousemove', function (e) {
      var rect = frame.getBoundingClientRect();
      var x = ((e.clientX - rect.left) / rect.width) * 100;
      var y = ((e.clientY - rect.top) / rect.height) * 100;
      main.style.transformOrigin = x + '% ' + y + '%';
    });
    frame.addEventListener('mouseleave', function () {
      main.style.transformOrigin = 'center center';
    });
  }
})();
