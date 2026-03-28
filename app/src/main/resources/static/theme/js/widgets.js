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
 * Widgets JavaScript - Dashboard widget functionality
 */

(function() {
  'use strict';

  // DOM Ready
  document.addEventListener('DOMContentLoaded', function() {
    initWidgetActions();
    initWidgetRefresh();
  });

  /**
   * Initialize widget action buttons
   */
  function initWidgetActions() {
    document.addEventListener('click', function(e) {
      const action = e.target.closest('[data-widget-action]');

      if (action) {
        e.preventDefault();
        const widget = action.closest('.widget, .card');
        const actionType = action.dataset.widgetAction;

        switch (actionType) {
          case 'refresh':
            refreshWidget(widget, action);
            break;
          case 'expand':
            toggleWidgetExpand(widget);
            break;
          case 'collapse':
            toggleWidgetCollapse(widget);
            break;
          case 'remove':
            removeWidget(widget);
            break;
          case 'settings':
            // Custom event for settings
            widget.dispatchEvent(new CustomEvent('widget:settings', {
              bubbles: true
            }));
            break;
        }
      }
    });
  }

  /**
   * Initialize auto-refresh widgets
   */
  function initWidgetRefresh() {
    const autoRefreshWidgets = document.querySelectorAll('[data-widget-refresh]');

    autoRefreshWidgets.forEach(function(widget) {
      const interval = parseInt(widget.dataset.widgetRefresh, 10);

      if (interval > 0) {
        setInterval(function() {
          const refreshBtn = widget.querySelector('[data-widget-action="refresh"]');
          if (refreshBtn) {
            refreshWidget(widget, refreshBtn);
          }
        }, interval * 1000);
      }
    });
  }

  /**
   * Refresh widget content
   * @param {HTMLElement} widget - The widget element
   * @param {HTMLElement} button - The refresh button
   */
  function refreshWidget(widget, button) {
    const widgetBody = widget.querySelector('.widget-body, .card-body');
    const icon = button?.querySelector('i');

    // Add loading state
    widget.classList.add('widget-loading');
    if (icon) {
      icon.classList.add('animate-spin');
    }

    // Dispatch refresh event for custom handling
    const event = new CustomEvent('widget:refresh', {
      bubbles: true,
      detail: {
        widget: widget
      }
    });
    widget.dispatchEvent(event);

    // Default: simulate loading delay
    setTimeout(function() {
      widget.classList.remove('widget-loading');
      if (icon) {
        icon.classList.remove('animate-spin');
      }

      // Dispatch refresh complete event
      widget.dispatchEvent(new CustomEvent('widget:refreshed', {
        bubbles: true
      }));
    }, 1000);
  }

  /**
   * Toggle widget expand (fullscreen)
   * @param {HTMLElement} widget - The widget element
   */
  function toggleWidgetExpand(widget) {
    widget.classList.toggle('widget-expanded');

    if (widget.classList.contains('widget-expanded')) {
      // Save original position
      widget.dataset.originalPosition = JSON.stringify({
        position: widget.style.position,
        top: widget.style.top,
        left: widget.style.left,
        width: widget.style.width,
        height: widget.style.height,
        zIndex: widget.style.zIndex
      });

      // Expand to fullscreen
      widget.style.position = 'fixed';
      widget.style.top = '0';
      widget.style.left = '0';
      widget.style.width = '100vw';
      widget.style.height = '100vh';
      widget.style.zIndex = '9999';
      widget.style.borderRadius = '0';

      document.body.style.overflow = 'hidden';
    } else {
      // Restore original position
      const original = JSON.parse(widget.dataset.originalPosition || '{}');
      Object.keys(original).forEach(function(key) {
        widget.style[key] = original[key] || '';
      });

      document.body.style.overflow = '';
    }
  }

  /**
   * Toggle widget collapse (minimize body)
   * @param {HTMLElement} widget - The widget element
   */
  function toggleWidgetCollapse(widget) {
    const body = widget.querySelector('.widget-body, .card-body');
    const icon = widget.querySelector('[data-widget-action="collapse"] i');

    if (body) {
      widget.classList.toggle('widget-collapsed');

      if (widget.classList.contains('widget-collapsed')) {
        body.style.maxHeight = body.scrollHeight + 'px';
        requestAnimationFrame(function() {
          body.style.maxHeight = '0';
          body.style.overflow = 'hidden';
          body.style.paddingTop = '0';
          body.style.paddingBottom = '0';
        });
        if (icon) {
          icon.style.transform = 'rotate(180deg)';
        }
      } else {
        body.style.maxHeight = body.scrollHeight + 'px';
        body.style.paddingTop = '';
        body.style.paddingBottom = '';
        setTimeout(function() {
          body.style.maxHeight = '';
          body.style.overflow = '';
        }, 300);
        if (icon) {
          icon.style.transform = '';
        }
      }
    }
  }

  /**
   * Remove widget with animation
   * @param {HTMLElement} widget - The widget element
   */
  function removeWidget(widget) {
    widget.style.transition = 'all 0.3s ease';
    widget.style.opacity = '0';
    widget.style.transform = 'scale(0.9)';

    setTimeout(function() {
      widget.remove();

      // Dispatch remove event
      document.dispatchEvent(new CustomEvent('widget:removed', {
        detail: {
          widgetId: widget.id || null
        }
      }));
    }, 300);
  }

  /**
   * Public API
   */
  window.Widgets = {
    /**
     * Refresh a specific widget
     * @param {string|HTMLElement} selector - Widget selector or element
     */
    refresh: function(selector) {
      const widget = typeof selector === 'string' ?
        document.querySelector(selector) :
        selector;

      if (widget) {
        const refreshBtn = widget.querySelector('[data-widget-action="refresh"]');
        refreshWidget(widget, refreshBtn);
      }
    },

    /**
     * Expand a widget
     * @param {string|HTMLElement} selector
     */
    expand: function(selector) {
      const widget = typeof selector === 'string' ?
        document.querySelector(selector) :
        selector;

      if (widget && !widget.classList.contains('widget-expanded')) {
        toggleWidgetExpand(widget);
      }
    },

    /**
     * Collapse a widget
     * @param {string|HTMLElement} selector
     */
    collapse: function(selector) {
      const widget = typeof selector === 'string' ?
        document.querySelector(selector) :
        selector;

      if (widget && !widget.classList.contains('widget-collapsed')) {
        toggleWidgetCollapse(widget);
      }
    },

    /**
     * Remove a widget
     * @param {string|HTMLElement} selector
     */
    remove: function(selector) {
      const widget = typeof selector === 'string' ?
        document.querySelector(selector) :
        selector;

      if (widget) {
        removeWidget(widget);
      }
    },

    /**
     * Set loading state
     * @param {string|HTMLElement} selector
     * @param {boolean} isLoading
     */
    setLoading: function(selector, isLoading) {
      const widget = typeof selector === 'string' ?
        document.querySelector(selector) :
        selector;

      if (widget) {
        if (isLoading) {
          widget.classList.add('widget-loading');
        } else {
          widget.classList.remove('widget-loading');
        }
      }
    }
  };

})();