/*
 * Глобальный публичный скрипт (подключается в layout, defer):
 *  — состояние шапки при скролле (is-scrolled)
 *  — мобильное меню: закрытие по клику/Escape
 *  — фильтры-шторка на мобиле (drawer + backdrop)
 *  — scroll-reveal секций/карточек (IntersectionObserver, один раз, со stagger)
 *  — кнопка «наверх» после 600px
 *  — плавное проявление картинок по load (скелетон → is-loaded)
 * Только transform/opacity. Уважает prefers-reduced-motion.
 */
(function () {
  'use strict';

  var reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  /* ── Переключатель темы ───────────────────────────────────────────────
     Тёмная — по умолчанию (без атрибута). Светлая — data-theme="light" на
     <html>, ставится ещё в <head> (защита от FOUC) и здесь по клику.
     Выбор хранится в localStorage. */
  var themeToggle = document.querySelector('.theme-toggle');
  if (themeToggle) {
    var root = document.documentElement;
    var themeMeta = document.querySelector('meta[name="theme-color"]');
    var applyTheme = function (theme) {
      var isLight = theme === 'light';
      if (isLight) root.setAttribute('data-theme', 'light');
      else root.removeAttribute('data-theme');
      themeToggle.setAttribute('aria-pressed', isLight ? 'true' : 'false');
      if (themeMeta) themeMeta.content = isLight ? '#faf7fc' : '#0f0b14';
    };
    // Синхронизируем состояние кнопки с темой, выставленной head-скриптом.
    applyTheme(root.getAttribute('data-theme') === 'light' ? 'light' : 'dark');
    themeToggle.addEventListener('click', function () {
      var next = root.getAttribute('data-theme') === 'light' ? 'dark' : 'light';
      applyTheme(next);
      try {
        localStorage.setItem('theme', next);
      } catch (e) {}
    });
  }

  /* ── Шапка: компактное состояние при скролле ─────────────────────────── */
  var header = document.querySelector('.site-header');
  if (header) {
    var onScrollHeader = function () {
      header.classList.toggle('is-scrolled', window.scrollY > 8);
    };
    onScrollHeader();
    window.addEventListener('scroll', onScrollHeader, { passive: true });

    /* Мобильное меню: закрытие по клику по ссылке и по Escape.
       (Открытие/закрытие по бургеру — инлайновый onclick в header.ejs.) */
    header.querySelectorAll('.site-nav__link').forEach(function (link) {
      link.addEventListener('click', function () {
        header.classList.remove('is-open');
        var t = header.querySelector('.nav-toggle');
        if (t) t.setAttribute('aria-expanded', 'false');
      });
    });
    document.addEventListener('keydown', function (e) {
      if (e.key === 'Escape') header.classList.remove('is-open');
    });
  }

  /* ── Фильтры-шторка (мобайл) ──────────────────────────────────────────── */
  var filters = document.querySelector('.catalog-filters');
  var filtersToggle = document.querySelector('[data-filters-toggle]');
  if (filters && filtersToggle) {
    var backdrop = document.createElement('div');
    backdrop.className = 'filters-backdrop';
    document.body.appendChild(backdrop);

    var openFilters = function () {
      filters.classList.add('is-open');
      backdrop.classList.add('is-open');
    };
    var closeFilters = function () {
      filters.classList.remove('is-open');
      backdrop.classList.remove('is-open');
    };
    filtersToggle.addEventListener('click', openFilters);
    backdrop.addEventListener('click', closeFilters);
    document.addEventListener('keydown', function (e) {
      if (e.key === 'Escape') closeFilters();
    });
  }

  /* ── Scroll-reveal ────────────────────────────────────────────────────── */
  var revealSelector =
    '[data-reveal], .product-card, .tile, .adv-item, .home-section__head, .marquee, .contact__info, .contact-form';
  var revealEls = Array.prototype.slice.call(document.querySelectorAll(revealSelector));

  if (reduceMotion || !('IntersectionObserver' in window)) {
    revealEls.forEach(function (el) {
      el.classList.add('is-inview');
    });
  } else {
    revealEls.forEach(function (el) {
      el.classList.add('reveal');
      // stagger по позиции внутри родителя (каскад в сетках)
      var parent = el.parentElement;
      if (parent) {
        var siblings = Array.prototype.filter.call(parent.children, function (c) {
          return c.classList.contains('reveal');
        });
        var idx = siblings.indexOf(el);
        el.style.setProperty('--i', Math.min(idx, 8));
      }
    });

    var io = new IntersectionObserver(
      function (entries, obs) {
        entries.forEach(function (entry) {
          if (entry.isIntersecting) {
            entry.target.classList.add('is-inview');
            obs.unobserve(entry.target);
          }
        });
      },
      { threshold: 0.15, rootMargin: '0px 0px -8% 0px' }
    );
    revealEls.forEach(function (el) {
      io.observe(el);
    });
  }

  /* ── Кнопка «наверх» ──────────────────────────────────────────────────── */
  var toTop = document.querySelector('.to-top');
  if (toTop) {
    var onScrollTop = function () {
      toTop.classList.toggle('is-visible', window.scrollY > 600);
    };
    onScrollTop();
    window.addEventListener('scroll', onScrollTop, { passive: true });
    toTop.addEventListener('click', function () {
      window.scrollTo({ top: 0, behavior: reduceMotion ? 'auto' : 'smooth' });
    });
  }

  /* ── Проявление картинок по load ──────────────────────────────────────── */
  document.querySelectorAll('.product-card__media img').forEach(function (img) {
    if (img.complete && img.naturalWidth > 0) {
      img.classList.add('is-loaded');
    } else {
      img.addEventListener('load', function () {
        img.classList.add('is-loaded');
      });
      img.addEventListener('error', function () {
        img.classList.add('is-loaded');
      });
    }
  });
})();
