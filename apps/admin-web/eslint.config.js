import js from '@eslint/js';
import ts from 'typescript-eslint';
import globals from 'globals';
export default ts.config({ ignores: ['dist'] }, js.configs.recommended, ...ts.configs.recommended, {
  files: ['**/*.{ts,tsx,js}'], languageOptions: { globals: globals.browser }
});
