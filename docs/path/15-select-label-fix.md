# 15. Select コンポーネントの表示名修正

## プロンプト

> 社員編集フォームで部署名とロールの表示がコードになってしまっています。修正してください。

## 問題

社員編集ダイアログで:
- **部署**: UUID（`a0000000-0000-0000-0000-000000000001`）がそのまま表示される
- **ロール**: コード値（`ADMIN`）がそのまま表示される

ドロップダウンを開くと正しい名前が見えるが、閉じた状態（トリガー）では `value` が表示されていた。

## 原因

Base UI（`@base-ui/react/select` v1.5）の `SelectValue` は、`children` を指定しない場合、
選択中の `value` をそのままテキストとして表示する。
`SelectItem` の `label` プロップは keyboard navigation 用であり、トリガーの表示テキストには影響しない。

## 修正内容

`SelectValue` に render function を渡し、`value` → 表示名の変換を行う:

```tsx
// Before
<SelectValue placeholder="部署を選択" />

// After
<SelectValue placeholder="部署を選択">
  {(value: string | null) => {
    const dept = departments.find((d) => d.id === value);
    return dept?.name ?? "部署を選択";
  }}
</SelectValue>
```

## 修正ファイル

- `packages/frontend/src/features/employee/EmployeeFormDialog.tsx` — 部署・ロール
- `packages/frontend/src/features/employee/EmployeeFilters.tsx` — フィルターの部署・ロール
- `packages/frontend/src/features/report/DepartmentFilter.tsx` — レポート部署フィルター
- `packages/frontend/src/features/correction/CorrectionList.tsx` — 修正申請ステータスフィルター

## つまずき

1. 最初に `SelectItem` の `label` プロップで解決できると思ったが、これは keyboard navigation 用
2. Base UI の `SelectValue` は `children` に render function `(value) => ReactNode` を受け取る API

## 教訓

Base UI の Select でトリガーの表示テキストをカスタマイズするには `SelectValue` の `children` に
render function を渡す。`SelectItem` の `label` は表示用ではなくキーボードナビゲーション用。
