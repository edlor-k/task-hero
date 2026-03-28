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
 * App Sidebar Toggle
 *
 * Unified sidebar toggle functionality for all app pages.
 * Automatically detects the app type based on container class and sets up toggle behavior.
 *
 * Usage: Include this script on any app page with a sidebar.
 * The script will automatically find the sidebar elements based on naming conventions.
 */

(function() {
  'use strict';

  document.addEventListener('DOMContentLoaded', function() {
    // Detect app type by looking for known container classes
    const appConfigs = [{
        container: '.email-container',
        prefix: 'email'
      },
      {
        container: '.chat-container',
        prefix: 'chat'
      },
      {
        container: '.contacts-container',
        prefix: 'contacts'
      },
      {
        container: '.calendar-app',
        prefix: 'calendar'
      },
      {
        container: '.file-manager',
        prefix: 'fm'
      },
      {
        container: '.todo-container',
        prefix: 'todo'
      },
      {
        container: '.support-container',
        prefix: 'support'
      }
    ];

    // Find which app we're on
    let activeConfig = null;
    for (const config of appConfigs) {
      if (document.querySelector(config.container)) {
        activeConfig = config;
        break;
      }
    }

    if (!activeConfig) {
      return; // Not an app page
    }

    const prefix = activeConfig.prefix;

    // Get elements using the detected prefix
    const sidebar = document.getElementById(`${prefix}Sidebar`);
    const sidebarToggle = document.getElementById(`${prefix}SidebarToggle`);
    const sidebarOverlay = document.getElementById(`${prefix}SidebarOverlay`);
    const sidebarClose = document.getElementById(`${prefix}SidebarClose`);

    if (!sidebar) {
      return; // No sidebar found
    }

    function openSidebar() {
      sidebar.classList.add('show');
      if (sidebarOverlay) {
        sidebarOverlay.classList.add('show');
      }
      document.body.classList.add(`${prefix}-sidebar-open`);
    }

    function closeSidebar() {
      sidebar.classList.remove('show');
      if (sidebarOverlay) {
        sidebarOverlay.classList.remove('show');
      }
      document.body.classList.remove(`${prefix}-sidebar-open`);
    }

    // Toggle button click
    if (sidebarToggle) {
      sidebarToggle.addEventListener('click', function() {
        if (sidebar.classList.contains('show')) {
          closeSidebar();
        } else {
          openSidebar();
        }
      });
    }

    // Overlay click to close
    if (sidebarOverlay) {
      sidebarOverlay.addEventListener('click', closeSidebar);
    }

    // Close button click
    if (sidebarClose) {
      sidebarClose.addEventListener('click', closeSidebar);
    }

    // Close on escape key
    document.addEventListener('keydown', function(e) {
      if (e.key === 'Escape' && sidebar.classList.contains('show')) {
        closeSidebar();
      }
    });

    // Close sidebar when selecting a nav item on mobile (breakpoint: 1280px)
    const navSelectors = [
      '.email-nav-item',
      '.chat-item',
      '.contacts-nav-item',
      '.contacts-group-item',
      '.calendar-sidebar .card', // For calendar, clicking cards
      '.fm-nav-item',
      '.todo-nav-item',
      '.todo-project-item',
      '.support-nav-item'
    ];

    navSelectors.forEach(selector => {
      document.querySelectorAll(selector).forEach(item => {
        item.addEventListener('click', function() {
          if (window.innerWidth < 1280 && sidebar.classList.contains('show')) {
            closeSidebar();
          }
        });
      });
    });

    // Expose functions globally for any custom handling
    window.appSidebar = {
      open: openSidebar,
      close: closeSidebar,
      toggle: function() {
        if (sidebar.classList.contains('show')) {
          closeSidebar();
        } else {
          openSidebar();
        }
      }
    };
  });
})();