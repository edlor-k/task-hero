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
 * Forms JavaScript - Form utilities, wizard, repeater, validation
 */

(function() {
  'use strict';

  // DOM Ready
  document.addEventListener('DOMContentLoaded', function() {
    initFormWizard();
    initFormRepeater();
    initPasswordToggle();
    initFloatingLabels();
    initFormValidation();
  });

  /**
   * Multi-step Form Wizard
   */
  function initFormWizard() {
    const wizards = document.querySelectorAll('[data-wizard]');

    wizards.forEach(function(wizard) {
      const steps = wizard.querySelectorAll('[data-wizard-step]');
      const panels = wizard.querySelectorAll('[data-wizard-panel]');
      const prevBtn = wizard.querySelector('[data-wizard-prev]');
      const nextBtn = wizard.querySelector('[data-wizard-next]');
      const submitBtn = wizard.querySelector('[data-wizard-submit]');

      let currentStep = 0;

      function showStep(index) {
        // Update steps
        steps.forEach(function(step, i) {
          step.classList.remove('active', 'completed');
          if (i < index) {
            step.classList.add('completed');
          } else if (i === index) {
            step.classList.add('active');
          }
        });

        // Update panels
        panels.forEach(function(panel, i) {
          panel.style.display = i === index ? 'block' : 'none';
        });

        // Update buttons
        if (prevBtn) {
          prevBtn.style.display = index === 0 ? 'none' : '';
        }
        if (nextBtn) {
          nextBtn.style.display = index === panels.length - 1 ? 'none' : '';
        }
        if (submitBtn) {
          submitBtn.style.display = index === panels.length - 1 ? '' : 'none';
        }

        // Dispatch event
        wizard.dispatchEvent(new CustomEvent('wizard:step', {
          detail: {
            step: index,
            total: panels.length
          }
        }));
      }

      function validateCurrentStep() {
        const currentPanel = panels[currentStep];
        const requiredFields = currentPanel.querySelectorAll('[required]');
        let isValid = true;

        requiredFields.forEach(function(field) {
          if (!field.value.trim()) {
            field.classList.add('is-invalid');
            isValid = false;
          } else {
            field.classList.remove('is-invalid');
          }
        });

        return isValid;
      }

      // Previous button
      if (prevBtn) {
        prevBtn.addEventListener('click', function(e) {
          e.preventDefault();
          if (currentStep > 0) {
            currentStep--;
            showStep(currentStep);
          }
        });
      }

      // Next button
      if (nextBtn) {
        nextBtn.addEventListener('click', function(e) {
          e.preventDefault();
          if (validateCurrentStep() && currentStep < panels.length - 1) {
            currentStep++;
            showStep(currentStep);
          }
        });
      }

      // Step click
      steps.forEach(function(step, index) {
        step.addEventListener('click', function() {
          // Only allow going back or to completed steps
          if (index < currentStep || step.classList.contains('completed')) {
            currentStep = index;
            showStep(currentStep);
          }
        });
      });

      // Initialize
      showStep(0);
    });
  }

  /**
   * Form Repeater - Add/remove form field groups
   */
  function initFormRepeater() {
    document.addEventListener('click', function(e) {
      // Add button
      const addBtn = e.target.closest('[data-repeater-add]');
      if (addBtn) {
        e.preventDefault();
        const repeater = addBtn.closest('[data-repeater]');
        const list = repeater.querySelector('[data-repeater-list]');
        const template = repeater.querySelector('[data-repeater-template]');

        if (list && template) {
          const newItem = template.content.cloneNode(true);
          const items = list.querySelectorAll('[data-repeater-item]');
          const index = items.length;

          // Update field names/ids with index
          newItem.querySelectorAll('[name]').forEach(function(field) {
            field.name = field.name.replace(/\[\d*\]/, '[' + index + ']');
          });

          newItem.querySelectorAll('[id]').forEach(function(field) {
            field.id = field.id.replace(/-\d+$/, '-' + index);
          });

          list.appendChild(newItem);

          // Dispatch event
          repeater.dispatchEvent(new CustomEvent('repeater:added', {
            detail: {
              index: index
            }
          }));
        }
      }

      // Remove button
      const removeBtn = e.target.closest('[data-repeater-remove]');
      if (removeBtn) {
        e.preventDefault();
        const item = removeBtn.closest('[data-repeater-item]');
        const repeater = removeBtn.closest('[data-repeater]');

        if (item) {
          item.style.transition = 'all 0.3s ease';
          item.style.opacity = '0';
          item.style.height = item.offsetHeight + 'px';

          requestAnimationFrame(function() {
            item.style.height = '0';
            item.style.paddingTop = '0';
            item.style.paddingBottom = '0';
            item.style.marginTop = '0';
            item.style.marginBottom = '0';
          });

          setTimeout(function() {
            item.remove();
            if (repeater) {
              repeater.dispatchEvent(new CustomEvent('repeater:removed'));
            }
          }, 300);
        }
      }
    });
  }

  /**
   * Password visibility toggle
   */
  function initPasswordToggle() {
    const toggles = document.querySelectorAll('.password-toggle');

    toggles.forEach(function(toggle) {
      toggle.addEventListener('click', function(e) {
        e.preventDefault();
        const input = this.parentElement.querySelector('input');
        const icon = this.querySelector('i');

        if (input) {
          if (input.type === 'password') {
            input.type = 'text';
            if (icon) {
              icon.classList.remove('bi-eye');
              icon.classList.add('bi-eye-slash');
            }
          } else {
            input.type = 'password';
            if (icon) {
              icon.classList.remove('bi-eye-slash');
              icon.classList.add('bi-eye');
            }
          }
        }
      });
    });
  }

  /**
   * Floating labels enhancement
   */
  function initFloatingLabels() {
    const floatingInputs = document.querySelectorAll('.form-floating input, .form-floating textarea');

    floatingInputs.forEach(function(input) {
      // Check initial state
      if (input.value) {
        input.classList.add('has-value');
      }

      input.addEventListener('input', function() {
        if (this.value) {
          this.classList.add('has-value');
        } else {
          this.classList.remove('has-value');
        }
      });
    });
  }

  /**
   * Form validation
   */
  function initFormValidation() {
    const forms = document.querySelectorAll('[data-validate]');

    forms.forEach(function(form) {
      form.addEventListener('submit', function(e) {
        if (!form.checkValidity()) {
          e.preventDefault();
          e.stopPropagation();
        }

        form.classList.add('was-validated');
      });

      // Real-time validation
      const inputs = form.querySelectorAll('input, textarea, select');
      inputs.forEach(function(input) {
        input.addEventListener('blur', function() {
          validateField(this);
        });

        input.addEventListener('input', function() {
          if (form.classList.contains('was-validated')) {
            validateField(this);
          }
        });
      });
    });
  }

  /**
   * Validate a single field
   * @param {HTMLElement} field
   */
  function validateField(field) {
    const isValid = field.checkValidity();

    if (isValid) {
      field.classList.remove('is-invalid');
      field.classList.add('is-valid');
    } else {
      field.classList.remove('is-valid');
      field.classList.add('is-invalid');
    }
  }

  /**
   * Public API
   */
  window.Forms = {
    /**
     * Validate a form
     * @param {string|HTMLElement} selector
     * @returns {boolean}
     */
    validate: function(selector) {
      const form = typeof selector === 'string' ?
        document.querySelector(selector) :
        selector;

      if (form) {
        form.classList.add('was-validated');
        return form.checkValidity();
      }
      return false;
    },

    /**
     * Reset form validation state
     * @param {string|HTMLElement} selector
     */
    resetValidation: function(selector) {
      const form = typeof selector === 'string' ?
        document.querySelector(selector) :
        selector;

      if (form) {
        form.classList.remove('was-validated');
        form.querySelectorAll('.is-valid, .is-invalid').forEach(function(field) {
          field.classList.remove('is-valid', 'is-invalid');
        });
      }
    },

    /**
     * Go to a specific wizard step
     * @param {string|HTMLElement} wizardSelector
     * @param {number} step
     */
    wizardGoTo: function(wizardSelector, step) {
      const wizard = typeof wizardSelector === 'string' ?
        document.querySelector(wizardSelector) :
        wizardSelector;

      if (wizard) {
        const steps = wizard.querySelectorAll('[data-wizard-step]');
        if (steps[step]) {
          steps[step].click();
        }
      }
    }
  };

})();