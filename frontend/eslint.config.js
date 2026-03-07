import js from '@eslint/js'
import tseslint from 'typescript-eslint'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'

export default tseslint.config(
  { ignores: ['dist'] },

  js.configs.recommended,
  ...tseslint.configs.recommended,

  {
    files: ['src/**/*.{ts,tsx}'],

    plugins: {
      'react-hooks': reactHooks,
      'react-refresh': reactRefresh,
    },

    rules: {
      // ---- React Hooks ----
      ...reactHooks.configs.recommended.rules,

      // ---- React Refresh ----
      // allowExportNames: context 檔案同時 export hook + provider 是正常模式
      'react-refresh/only-export-components': [
        'warn',
        {
          allowConstantExport: true,
          allowExportNames: ['useAuth'],
        },
      ],

      // ---- TypeScript ----
      // 禁止 any，要明確標記型別
      '@typescript-eslint/no-explicit-any': 'error',

      // 未使用的變數報錯（底線開頭視為刻意忽略）
      '@typescript-eslint/no-unused-vars': [
        'error',
        { argsIgnorePattern: '^_', varsIgnorePattern: '^_' },
      ],

      // 一致使用 import type
      '@typescript-eslint/consistent-type-imports': [
        'error',
        { prefer: 'type-imports', fixStyle: 'inline-type-imports' },
      ],

      // ---- 一般 JS ----
      // 禁止 console.log，只允許 warn / error
      'no-console': ['warn', { allow: ['warn', 'error'] }],

      // 優先用 const
      'prefer-const': 'error',
    },
  }
)
