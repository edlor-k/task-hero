/**
 * =======================================================
 * Template Name: ModernAdmin - Bootstrap Admin Template
 * Template URL:  https://bootstrapmade.com/modern-admin-bootstrap-html-admin-template/
 * Updated: Mar 4, 2026
 * Author: BootstrapMade.com
 * License: https://bootstrapmade.com/license/
 * =======================================================
 */
/**
 * Theme JavaScript - Dark mode toggle and persistence
 */
(function() {
  'use strict';

  const THEME_KEY = 'theme';
  const DARK_THEME = 'dark';
  const LIGHT_THEME = 'light';
  const mql = window.matchMedia('(prefers-color-scheme: dark)');

  initTheme();
  document.addEventListener('DOMContentLoaded', initThemeToggle);

  function getStoredTheme() {
    try {
      return localStorage.getItem(THEME_KEY);
    } catch (_) {
      return null;
    }
  }

  function setStoredTheme(theme) {
    try {
      localStorage.setItem(THEME_KEY, theme);
    } catch (_) {
      // ignore storage errors (e.g., privacy mode)
    }
  }

  function setTheme(theme) {
    if (theme === DARK_THEME) {
      document.documentElement.setAttribute('data-theme', DARK_THEME);
    } else {
      document.documentElement.removeAttribute('data-theme');
    }
  }

  function getTheme() {
    return document.documentElement.getAttribute('data-theme') || LIGHT_THEME;
  }

  function initTheme() {
    const savedTheme = getStoredTheme();
    if (savedTheme) {
      setTheme(savedTheme);
    } else {
      setTheme(mql.matches ? DARK_THEME : LIGHT_THEME);
    }

    const handleChange = function(e) {
      if (!getStoredTheme()) {
        setTheme(e.matches ? DARK_THEME : LIGHT_THEME);
      }
    };

    if (mql.addEventListener) {
      mql.addEventListener('change', handleChange);
    } else if (mql.addListener) {
      mql.addListener(handleChange);
    }
  }

  function initThemeToggle() {
    document.querySelectorAll('.theme-toggle').forEach(function(toggle) {
      toggle.addEventListener('click', function(e) {
        e.preventDefault();
        const newTheme = getTheme() === DARK_THEME ? LIGHT_THEME : DARK_THEME;
        setTheme(newTheme);
        setStoredTheme(newTheme);
      });
    });
  }

  window.Theme = {
    toggle: function() {
      const newTheme = getTheme() === DARK_THEME ? LIGHT_THEME : DARK_THEME;
      setTheme(newTheme);
      setStoredTheme(newTheme);
    },
    setDark: function() {
      setTheme(DARK_THEME);
      setStoredTheme(DARK_THEME);
    },
    setLight: function() {
      setTheme(LIGHT_THEME);
      setStoredTheme(LIGHT_THEME);
    },
    isDark: function() {
      return getTheme() === DARK_THEME;
    },
    getTheme
  };
})();