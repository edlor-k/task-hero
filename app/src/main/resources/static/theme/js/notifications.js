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
 * Notifications JavaScript - Notification center functionality
 */

(function() {
  'use strict';

  // DOM Ready
  document.addEventListener('DOMContentLoaded', function() {
    initNotifications();
  });

  /**
   * Initialize notification functionality
   */
  function initNotifications() {
    initMarkAsRead();
    initClearAll();
    initNotificationActions();
  }

  /**
   * Mark individual notification as read
   */
  function initMarkAsRead() {
    document.addEventListener('click', function(e) {
      const notificationItem = e.target.closest('.notification-item');

      if (notificationItem && notificationItem.classList.contains('unread')) {
        // Mark as read on click
        notificationItem.classList.remove('unread');
        updateBadgeCount(-1);
      }
    });
  }

  /**
   * Mark all notifications as read
   */
  function initClearAll() {
    const markAllReadBtn = document.querySelector('[data-notification-action="mark-all-read"]');

    if (markAllReadBtn) {
      markAllReadBtn.addEventListener('click', function(e) {
        e.preventDefault();

        const unreadItems = document.querySelectorAll('.notification-item.unread');
        unreadItems.forEach(function(item) {
          item.classList.remove('unread');
        });

        updateBadgeCount(-unreadItems.length);
      });
    }
  }

  /**
   * Notification action buttons (dismiss, etc.)
   */
  function initNotificationActions() {
    document.addEventListener('click', function(e) {
      const dismissBtn = e.target.closest('[data-notification-action="dismiss"]');

      if (dismissBtn) {
        e.preventDefault();
        e.stopPropagation();

        const notificationItem = dismissBtn.closest('.notification-item');
        if (notificationItem) {
          // Check if it was unread
          const wasUnread = notificationItem.classList.contains('unread');

          // Animate and remove
          notificationItem.style.height = notificationItem.offsetHeight + 'px';
          notificationItem.style.transition = 'all 0.3s ease';

          requestAnimationFrame(function() {
            notificationItem.style.height = '0';
            notificationItem.style.opacity = '0';
            notificationItem.style.paddingTop = '0';
            notificationItem.style.paddingBottom = '0';
            notificationItem.style.marginTop = '0';
            notificationItem.style.marginBottom = '0';
          });

          setTimeout(function() {
            notificationItem.remove();

            if (wasUnread) {
              updateBadgeCount(-1);
            }

            // Show empty state if no notifications
            checkEmptyState();
          }, 300);
        }
      }
    });
  }

  /**
   * Update notification badge count
   * @param {number} change - Amount to change (positive or negative)
   */
  function updateBadgeCount(change) {
    const badges = document.querySelectorAll('.notification-dropdown .badge');

    badges.forEach(function(badge) {
      let count = parseInt(badge.textContent, 10) || 0;
      count = Math.max(0, count + change);

      if (count > 0) {
        badge.textContent = count > 99 ? '99+' : count;
        badge.style.display = '';
      } else {
        badge.style.display = 'none';
      }
    });

    // Update header text
    const header = document.querySelector('.notification-header h6');
    if (header) {
      const count = parseInt(document.querySelector('.notification-dropdown .badge')?.textContent, 10) || 0;
      header.textContent = count > 0 ? `You have ${count} new notification${count !== 1 ? 's' : ''}` : 'No new notifications';
    }
  }

  /**
   * Check if notification list is empty and show empty state
   */
  function checkEmptyState() {
    const notificationList = document.querySelector('.notification-list');
    const items = notificationList?.querySelectorAll('.notification-item');

    if (notificationList && (!items || items.length === 0)) {
      notificationList.innerHTML = `
        <div class="notification-empty">
          <i class="bi bi-bell-slash"></i>
          <p>No notifications</p>
        </div>
      `;
    }
  }

  /**
   * Public API
   */
  window.Notifications = {
    /**
     * Add a new notification
     * @param {Object} options - Notification options
     */
    add: function(options) {
      const defaults = {
        type: 'info', // info, success, warning, danger
        title: 'Notification',
        message: '',
        time: 'Just now',
        link: '#'
      };

      const config = Object.assign({}, defaults, options);
      const notificationList = document.querySelector('.notification-list');

      if (!notificationList) return;

      const iconMap = {
        info: 'bi-info-circle text-primary',
        success: 'bi-check-circle text-success',
        warning: 'bi-exclamation-triangle text-warning',
        danger: 'bi-x-circle text-danger'
      };

      const html = `
        <div class="notification-item unread">
          <div class="notification-icon">
            <i class="bi ${iconMap[config.type] || iconMap.info}"></i>
          </div>
          <div class="notification-content">
            <div class="notification-title">${config.title}</div>
            <div class="notification-text">${config.message}</div>
            <div class="notification-time">${config.time}</div>
          </div>
          <button class="btn-icon" data-notification-action="dismiss">
            <i class="bi bi-x"></i>
          </button>
        </div>
      `;

      // Remove empty state if present
      const emptyState = notificationList.querySelector('.notification-empty');
      if (emptyState) {
        emptyState.remove();
      }

      // Add to top of list
      notificationList.insertAdjacentHTML('afterbegin', html);
      updateBadgeCount(1);
    },

    /**
     * Clear all notifications
     */
    clearAll: function() {
      const notificationList = document.querySelector('.notification-list');
      if (notificationList) {
        notificationList.innerHTML = `
          <div class="notification-empty">
            <i class="bi bi-bell-slash"></i>
            <p>No notifications</p>
          </div>
        `;
      }
      updateBadgeCount(-999); // Reset to 0
    },

    /**
     * Get notification count
     * @returns {number}
     */
    getCount: function() {
      const badge = document.querySelector('.notification-dropdown .badge');
      return parseInt(badge?.textContent, 10) || 0;
    }
  };

})();