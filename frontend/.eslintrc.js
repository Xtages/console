module.exports = {
  root: true,
  extends: ['airbnb-typescript'],
  plugins: [
    'eslint-plugin-tsdoc',
  ],
  parserOptions: {
    project: './tsconfig.json',
  },
  rules: {
    'tsdoc/syntax': 'warn',
    'object-curly-newline': ['error', {
      ImportDeclaration: 'never',
      ExportDeclaration: 'never',
    }],
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
    'react/require-default-props': 'off',
    'no-plusplus': 'off',
    'import/no-extraneous-dependencies': [
      'error', {
        devDependencies: ['**/*.stories.tsx', 'src/mocks/**/*.*'],
      },
    ],
    'import/prefer-default-export': 'off',
    '@typescript-eslint/no-unused-vars': ['error', {argsIgnorePattern: '^ignore'}],
  },
};
