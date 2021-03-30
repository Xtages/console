module.exports = {
  root: true,
  extends: ['airbnb-typescript'],
  parserOptions: {
    project: './tsconfig.json',
  },
  rules: {
    'object-curly-newline': ['error', {ImportDeclaration: 'never', ExportDeclaration: 'never'}],
    '@typescript-eslint/object-curly-spacing': ['error', 'never'],
    'jsx-a11y/label-has-associated-control': ['error', {
      required: {
        some: ['nesting', 'id'],
      },
    }],
    'jsx-a11y/label-has-for': ['error', {
      required: {
        some: ['nesting', 'id'],
      },
    }],
    'no-use-before-define': 'off',
    '@typescript-eslint/no-use-before-define': 'off',
    'react/jsx-props-no-spreading': 'off',
  },
};
