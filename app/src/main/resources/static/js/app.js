/**
 * TaskHero JavaScript
 */

document.addEventListener('DOMContentLoaded', function() {
    const dismissedNotificationPrefix = 'taskhero.dismissedNotification.';

    function getDismissedNotification(key) {
        try {
            return localStorage.getItem(dismissedNotificationPrefix + key) === '1';
        } catch (e) {
            return false;
        }
    }

    function setDismissedNotification(key) {
        try {
            localStorage.setItem(dismissedNotificationPrefix + key, '1');
        } catch (e) {
            // Если localStorage недоступен, кнопка все равно скроет текущее уведомление.
        }
    }

    document.querySelectorAll('[data-notification-key]').forEach(function(notification) {
        const key = notification.getAttribute('data-notification-key');
        if (!key) {
            return;
        }

        if (getDismissedNotification(key)) {
            notification.remove();
            return;
        }

        const closeBtn = notification.querySelector('.btn-close');
        if (closeBtn) {
            closeBtn.addEventListener('click', function() {
                setDismissedNotification(key);
            });
        }
    });

    function formatLocalDateTime(isoString) {
        const date = new Date(isoString);
        if (Number.isNaN(date.getTime())) {
            return isoString;
        }
        return new Intl.DateTimeFormat(undefined, {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit'
        }).format(date);
    }

    document.querySelectorAll('[data-utc-datetime]').forEach(function(element) {
        const value = element.getAttribute('data-utc-datetime');
        if (value) {
            element.textContent = formatLocalDateTime(value);
        }
    });

    document.querySelectorAll('[data-deadline-state]').forEach(function(element) {
        const value = element.getAttribute('data-deadline-state');
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) {
            return;
        }

        const hoursLeft = Math.trunc((date.getTime() - Date.now()) / 3600000);
        const label = element.querySelector('[data-deadline-label]');
        element.classList.remove('text-muted', 'text-danger', 'fw-semibold');
        if (hoursLeft < 0) {
            element.classList.add('text-danger', 'fw-semibold');
            if (label) {
                label.textContent = 'просрочено';
            }
        } else {
            element.classList.add('text-muted');
            if (label) {
                label.textContent = 'осталось ' + hoursLeft + ' ч';
            }
        }
    });

    // Автоматически скрываем уведомления через 5 секунд.
    const alerts = document.querySelectorAll('.alert-dismissible');
    alerts.forEach(function(alert) {
        setTimeout(function() {
            const closeBtn = alert.querySelector('.btn-close');
            if (closeBtn) {
                closeBtn.click();
            }
        }, 5000);
    });

    // Приводим токен ребенка к верхнему регистру.
    const tokenInput = document.getElementById('loginToken');
    if (tokenInput) {
        tokenInput.addEventListener('input', function() {
            this.value = this.value.toUpperCase();
        });
    }

    // Подтверждение удаления.
    const deleteButtons = document.querySelectorAll('[data-confirm]');
    deleteButtons.forEach(function(button) {
        button.addEventListener('click', function(e) {
            const message = this.getAttribute('data-confirm') || 'Вы уверены?';
            if (!confirm(message)) {
                e.preventDefault();
            }
        });
    });

    // Состояние загрузки для форм.
    const forms = document.querySelectorAll('form');
    forms.forEach(function(form) {
        form.addEventListener('submit', function() {
            form.querySelectorAll('input[type="datetime-local"][name]').forEach(function(input) {
                if (!input.value) {
                    return;
                }

                const hidden = document.createElement('input');
                hidden.type = 'hidden';
                hidden.name = input.name;
                hidden.value = new Date(input.value).toISOString();
                form.appendChild(hidden);
                input.disabled = true;
            });

            const submitBtn = form.querySelector('button[type="submit"]');
            if (submitBtn) {
                submitBtn.disabled = true;
                const originalText = submitBtn.innerHTML;
                submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Загрузка...';

                // Возвращаем кнопку в рабочее состояние через 5 секунд.
                setTimeout(function() {
                    submitBtn.disabled = false;
                    submitBtn.innerHTML = originalText;
                }, 5000);
            }
        });
    });

    // Инициализация Bootstrap tooltips, если они используются.
    const tooltipTriggerList = document.querySelectorAll('[data-bs-toggle="tooltip"]');
    if (tooltipTriggerList.length > 0 && typeof bootstrap !== 'undefined') {
        tooltipTriggerList.forEach(function(tooltipTriggerEl) {
            new bootstrap.Tooltip(tooltipTriggerEl);
        });
    }

    console.log('TaskHero App initialized');
});
