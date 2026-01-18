/**
 * Theme management module for the Debugger application.
 * Handles theme switching and persistence using localStorage.
 * 
 * @module theme
 */
(function() {
    'use strict';

    const THEME_STORAGE_KEY = 'theme';
    const THEME_ATTRIBUTE = 'data-theme';
    const ACTIVE_CLASS = 'active';

    /**
     * Gets the preferred theme from localStorage or system preferences.
     * 
     * @returns {string} The theme name
     */
    function getPreferredTheme() {
        const storedTheme = localStorage.getItem(THEME_STORAGE_KEY);
        if (storedTheme) {
            return storedTheme;
        }

        // Check system preference
        const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
        return prefersDark ? 'dark' : 'light';
    }

    /**
     * Applies the specified theme to the document.
     * 
     * @param {string} theme - The theme name to apply
     */
    function applyTheme(theme) {
        const root = document.documentElement;
        root.setAttribute(THEME_ATTRIBUTE, theme);
    }

    /**
     * Updates the active state of theme buttons.
     * 
     * @param {string} activeTheme - The currently active theme
     * @param {NodeList} buttons - The theme button elements
     */
    function updateActiveButtons(activeTheme, buttons) {
        buttons.forEach(button => {
            const isActive = button.dataset.seltheme === activeTheme;
            button.classList.toggle(ACTIVE_CLASS, isActive);
            button.setAttribute('aria-pressed', isActive.toString());
        });
    }

    /**
     * Handles theme button click events.
     * 
     * @param {Event} event - The click event
     * @param {NodeList} buttons - The theme button elements
     */
    function handleThemeButtonClick(event, buttons) {
        const button = event.currentTarget;
        const theme = button.dataset.seltheme;

        if (!theme) {
            console.warn('Theme button missing data-seltheme attribute');
            return;
        }

        try {
            applyTheme(theme);
            localStorage.setItem(THEME_STORAGE_KEY, theme);
            updateActiveButtons(theme, buttons);
        } catch (error) {
            console.error('Error changing theme:', error);
        }
    }

    /**
     * Initializes the theme system.
     */
    function init() {
        const root = document.documentElement;
        const buttons = document.querySelectorAll('.theme-picker [data-seltheme]');

        if (!buttons.length) {
            return;
        }

        // Apply saved theme or default
        const preferredTheme = getPreferredTheme();
        applyTheme(preferredTheme);
        updateActiveButtons(preferredTheme, buttons);

        // Attach click handlers
        buttons.forEach(button => {
            button.addEventListener('click', (event) => {
                handleThemeButtonClick(event, buttons);
            });
        });

        // Listen for system theme changes
        const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
        mediaQuery.addEventListener('change', (event) => {
            // Only apply system preference if no theme is stored
            if (!localStorage.getItem(THEME_STORAGE_KEY)) {
                const theme = event.matches ? 'dark' : 'light';
                applyTheme(theme);
                updateActiveButtons(theme, buttons);
            }
        });
    }

    // Initialize when DOM is ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();

