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
 * Main JavaScript - Core functionality
 * Handles sidebar, mobile menu, search, scroll to top, etc.
 */

(function() {
  'use strict';

  // DOM Ready
  document.addEventListener('DOMContentLoaded', function() {
    initSidebar();
    initSearch();
    initBackToTop();
    initDropdowns();
    initTooltips();
  });

  /**
   * Sidebar Toggle
   */
  function initSidebar() {
    const body = document.body;
    const sidebarToggle = document.querySelector('.sidebar-toggle');
    const sidebarClose = document.querySelector('.sidebar-close');
    const sidebarOverlay = document.querySelector('.sidebar-overlay');

    // Toggle sidebar on desktop (collapse/expand)
    if (sidebarToggle) {
      sidebarToggle.addEventListener('click', function(e) {
        e.preventDefault();

        if (window.innerWidth >= 1200) {
          // Desktop: Toggle collapsed state
          body.classList.toggle('sidebar-collapsed');
          localStorage.setItem('sidebar-collapsed', body.classList.contains('sidebar-collapsed'));
        } else {
          // Mobile: Toggle open state
          body.classList.toggle('sidebar-open');
        }
      });
    }

    // Close sidebar on mobile
    if (sidebarClose) {
      sidebarClose.addEventListener('click', function(e) {
        e.preventDefault();
        body.classList.remove('sidebar-open');
      });
    }

    // Close sidebar when clicking overlay
    if (sidebarOverlay) {
      sidebarOverlay.addEventListener('click', function() {
        body.classList.remove('sidebar-open');
      });
    }

    // Restore collapsed state from localStorage
    if (localStorage.getItem('sidebar-collapsed') === 'true' && window.innerWidth >= 1200) {
      body.classList.add('sidebar-collapsed');
    }

    // Handle window resize
    let resizeTimer;
    window.addEventListener('resize', function() {
      clearTimeout(resizeTimer);
      resizeTimer = setTimeout(function() {
        if (window.innerWidth >= 1200) {
          // Desktop: close mobile sidebar if open
          body.classList.remove('sidebar-open');
          // Restore collapsed state from localStorage if user had it enabled
          if (localStorage.getItem('sidebar-collapsed') === 'true') {
            body.classList.add('sidebar-collapsed');
          }
        } else {
          // Mobile: disable compact/collapsed sidebar (doesn't make sense on mobile)
          body.classList.remove('sidebar-collapsed');
        }
      }, 250);
    });

    // Initialize sidebar navigation
    initSidebarNav();
  }

  /**
   * Sidebar Navigation - Handle submenus
   */
  function initSidebarNav() {
    // Handle both top-level (.nav-item.has-submenu) and nested (.has-submenu) submenus
    const navLinks = document.querySelectorAll('.has-submenu > .nav-link');

    navLinks.forEach(function(link) {
      link.addEventListener('click', function(e) {
        e.preventDefault();

        const parent = this.parentElement;
        const submenu = parent.querySelector(':scope > .nav-submenu');

        // If sidebar is collapsed on desktop, don't toggle
        if (document.body.classList.contains('sidebar-collapsed') && window.innerWidth >= 1200) {
          return;
        }

        // Toggle this submenu
        const isOpen = parent.classList.contains('open');

        // Close other open submenus at the same level
        const siblings = parent.parentElement.querySelectorAll(':scope > .has-submenu.open');
        siblings.forEach(function(sibling) {
          if (sibling !== parent) {
            closeSubmenu(sibling);
          }
        });

        // Toggle current submenu
        if (isOpen) {
          closeSubmenu(parent);
        } else {
          openSubmenu(parent);
        }
      });
    });

    // Auto-expand active submenu on page load
    const activeItems = document.querySelectorAll('.nav-submenu .nav-link.active');
    activeItems.forEach(function(activeItem) {
      let parent = activeItem.closest('.has-submenu');
      while (parent) {
        openSubmenu(parent, false);
        parent = parent.parentElement.closest('.has-submenu');
      }
    });

    // Scroll to active nav item after submenus are expanded
    scrollToActiveNavItem();
  }

  /**
   * Scroll sidebar to center the active nav item if it's not visible
   */
  function scrollToActiveNavItem() {
    const sidebarNav = document.querySelector('.sidebar-nav');
    if (!sidebarNav) return;

    // Find the active nav link (could be top-level or inside a submenu)
    const activeLink = sidebarNav.querySelector('.nav-link.active');
    if (!activeLink) return;

    // Use requestAnimationFrame to ensure DOM has updated after submenu expansion
    requestAnimationFrame(function() {
      const sidebarRect = sidebarNav.getBoundingClientRect();
      const activeRect = activeLink.getBoundingClientRect();

      // Calculate the position of the active item relative to the sidebar
      const activeTop = activeRect.top - sidebarRect.top + sidebarNav.scrollTop;
      const activeBottom = activeTop + activeRect.height;

      // Check if the active item is fully visible
      const visibleTop = sidebarNav.scrollTop;
      const visibleBottom = visibleTop + sidebarNav.clientHeight;

      const isFullyVisible = activeTop >= visibleTop && activeBottom <= visibleBottom;

      if (!isFullyVisible) {
        // Calculate scroll position to center the active item
        const targetScrollTop = activeTop - (sidebarNav.clientHeight / 2) + (activeRect.height / 2);

        // Clamp to valid scroll range
        const maxScroll = sidebarNav.scrollHeight - sidebarNav.clientHeight;
        const clampedScrollTop = Math.max(0, Math.min(targetScrollTop, maxScroll));

        // Smooth scroll to the active item
        sidebarNav.scrollTo({
          top: clampedScrollTop,
          behavior: 'smooth'
        });
      }
    });
  }

  /**
   * Open a submenu
   */
  function openSubmenu(item, animate = true) {
    const link = item.querySelector(':scope > .nav-link');
    const submenu = item.querySelector(':scope > .nav-submenu');

    if (!submenu) return;

    // Remove Jinja-set .show class so JS controls maxHeight exclusively
    submenu.classList.remove('show');

    item.classList.add('open');
    if (link) {
      link.setAttribute('aria-expanded', 'true');
    }

    if (animate) {
      submenu.style.maxHeight = submenu.scrollHeight + 'px';
      // Update parent submenu height after child has expanded
      requestAnimationFrame(function() {
        updateParentHeight(item);
      });
    } else {
      submenu.style.maxHeight = 'none';
      updateParentHeight(item);
    }
  }

  /**
   * Close a submenu and its children
   */
  function closeSubmenu(item) {
    const link = item.querySelector(':scope > .nav-link');
    const submenu = item.querySelector(':scope > .nav-submenu');

    if (!submenu) return;

    item.classList.remove('open');
    if (link) {
      link.setAttribute('aria-expanded', 'false');
    }
    submenu.style.maxHeight = null;

    // Also close any nested open submenus
    const nestedOpen = item.querySelectorAll('.has-submenu.open');
    nestedOpen.forEach(function(nested) {
      nested.classList.remove('open');
      const nestedLink = nested.querySelector(':scope > .nav-link');
      const nestedSubmenu = nested.querySelector(':scope > .nav-submenu');
      if (nestedLink) {
        nestedLink.setAttribute('aria-expanded', 'false');
      }
      if (nestedSubmenu) {
        nestedSubmenu.style.maxHeight = null;
      }
    });
  }

  /**
   * Update parent submenu heights when nested submenu opens
   */
  function updateParentHeight(item) {
    let parent = item.parentElement.closest('.has-submenu.open');
    while (parent) {
      const parentSubmenu = parent.querySelector(':scope > .nav-submenu');
      if (parentSubmenu) {
        // Calculate total height including all nested open submenus
        let totalHeight = 0;
        const children = parentSubmenu.children;
        for (let i = 0; i < children.length; i++) {
          totalHeight += children[i].offsetHeight;
        }
        parentSubmenu.style.maxHeight = totalHeight + 'px';
      }
      parent = parent.parentElement.closest('.has-submenu.open');
    }
  }

  /**
   * Search Bar Toggle (Mobile)
   */
  function initSearch() {
    const searchToggle = document.querySelector('.search-toggle');
    const mobileSearch = document.querySelector('.mobile-search');
    const mobileMenuToggle = document.querySelector('.mobile-menu-toggle');
    const mobileHeaderMenu = document.querySelector('.mobile-header-menu');
    const searchInput = mobileSearch ? mobileSearch.querySelector('input') : null;

    // Search toggle
    if (searchToggle && mobileSearch) {
      searchToggle.addEventListener('click', function(e) {
        e.preventDefault();

        // Close mobile menu if open
        if (mobileHeaderMenu && mobileHeaderMenu.classList.contains('active')) {
          mobileHeaderMenu.classList.remove('active');
        }

        mobileSearch.classList.toggle('active');
        if (mobileSearch.classList.contains('active') && searchInput) {
          searchInput.focus();
        }
      });
    }

    // Mobile menu toggle (three dots)
    if (mobileMenuToggle && mobileHeaderMenu) {
      mobileMenuToggle.addEventListener('click', function(e) {
        e.preventDefault();

        // Close search if open
        if (mobileSearch && mobileSearch.classList.contains('active')) {
          mobileSearch.classList.remove('active');
        }

        mobileHeaderMenu.classList.toggle('active');
      });
    }

    // Close on click outside
    document.addEventListener('click', function(e) {
      // Close mobile search
      if (mobileSearch && !mobileSearch.contains(e.target) && !searchToggle.contains(e.target)) {
        mobileSearch.classList.remove('active');
      }

      // Close mobile header menu
      if (mobileHeaderMenu && mobileMenuToggle &&
        !mobileHeaderMenu.contains(e.target) && !mobileMenuToggle.contains(e.target)) {
        mobileHeaderMenu.classList.remove('active');
      }
    });

    // Close menus on window resize to desktop
    window.addEventListener('resize', function() {
      if (window.innerWidth >= 768) {
        if (mobileSearch) mobileSearch.classList.remove('active');
        if (mobileHeaderMenu) mobileHeaderMenu.classList.remove('active');
      }
    });
  }

  /**
   * Back to Top Button
   */
  function initBackToTop() {
    const backToTop = document.querySelector('.back-to-top');

    if (backToTop) {
      // Show/hide based on scroll position
      window.addEventListener('scroll', function() {
        if (window.scrollY > 100) {
          backToTop.classList.add('visible');
        } else {
          backToTop.classList.remove('visible');
        }
      });

      // Scroll to top on click
      backToTop.addEventListener('click', function(e) {
        e.preventDefault();
        window.scrollTo({
          top: 0,
          behavior: 'smooth'
        });
      });
    }
  }

  /**
   * Initialize Dropdowns (if not using Bootstrap JS)
   */
  function initDropdowns() {
    // Only initialize if Bootstrap's dropdown isn't available
    if (typeof bootstrap !== 'undefined' && bootstrap.Dropdown) {
      return;
    }

    const dropdownToggles = document.querySelectorAll('[data-bs-toggle="dropdown"]');

    dropdownToggles.forEach(function(toggle) {
      toggle.addEventListener('click', function(e) {
        e.preventDefault();
        e.stopPropagation();

        const parent = this.parentElement;
        const menu = parent.querySelector('.dropdown-menu');

        // Close other dropdowns
        document.querySelectorAll('.dropdown-menu.show').forEach(function(openMenu) {
          if (openMenu !== menu) {
            openMenu.classList.remove('show');
          }
        });

        // Toggle this dropdown
        menu.classList.toggle('show');
      });
    });

    // Close dropdowns on click outside
    document.addEventListener('click', function(e) {
      if (!e.target.closest('.dropdown')) {
        document.querySelectorAll('.dropdown-menu.show').forEach(function(menu) {
          menu.classList.remove('show');
        });
      }
    });
  }

  /**
   * Initialize Tooltips
   */
  function initTooltips() {
    // Only initialize if Bootstrap's tooltip is available
    if (typeof bootstrap !== 'undefined' && bootstrap.Tooltip) {
      const tooltipTriggerList = document.querySelectorAll('[data-bs-toggle="tooltip"]');
      tooltipTriggerList.forEach(function(tooltipTriggerEl) {
        new bootstrap.Tooltip(tooltipTriggerEl);
      });
    }
  }

  /**
   * Fullscreen Toggle
   */
  window.toggleFullscreen = function() {
    if (!document.fullscreenElement) {
      document.documentElement.requestFullscreen();
      document.body.classList.add('fullscreen-active');
    } else {
      if (document.exitFullscreen) {
        document.exitFullscreen();
        document.body.classList.remove('fullscreen-active');
      }
    }
  };

  // Listen for fullscreen change
  document.addEventListener('fullscreenchange', function() {
    if (!document.fullscreenElement) {
      document.body.classList.remove('fullscreen-active');
    }
  });

})();