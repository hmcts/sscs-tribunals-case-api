module.exports = {
  overrides: [
    {
      files: ['e2e/*.ts', 'e2e/**/*.ts'],
      parser: '@typescript-eslint/parser',
      parserOptions: {
        ecmaVersion: 'latest',
        sourceType: 'module',
      },
      extends: [
        'plugin:@typescript-eslint/recommended',
        'plugin:playwright/recommended'
      ],
    },
  ],
};