import { describe, expect, it } from "vitest";
import { MEMO_MAX_LENGTH, validateMemo } from "./memo-validation";

describe("validateMemo", () => {
  it("空文字はエラーなし", () => {
    expect(validateMemo("")).toBeNull();
  });

  it("20文字以内はエラーなし", () => {
    expect(validateMemo("あいうえおかきくけこさしすせそたちつてと")).toBeNull();
  });

  it("21文字以上はエラーメッセージを返す", () => {
    const result = validateMemo("あいうえおかきくけこさしすせそたちつてとな");
    expect(result).toBe(`メモは${MEMO_MAX_LENGTH}文字以内で入力してください`);
  });

  it("改行を含む場合はエラーメッセージを返す", () => {
    expect(validateMemo("テスト\nメモ")).toBe("改行・絵文字・HTMLタグは使用できません");
  });

  it("キャリッジリターンを含む場合はエラーメッセージを返す", () => {
    expect(validateMemo("テスト\rメモ")).toBe("改行・絵文字・HTMLタグは使用できません");
  });

  it("HTMLタグを含む場合はエラーメッセージを返す", () => {
    expect(validateMemo("<script>")).toBe("改行・絵文字・HTMLタグは使用できません");
  });

  it("絵文字を含む場合はエラーメッセージを返す", () => {
    expect(validateMemo("テスト😀")).toBe("改行・絵文字・HTMLタグは使用できません");
  });

  it("通常のテキストはエラーなし", () => {
    expect(validateMemo("電車遅延のため")).toBeNull();
  });

  it("記号を含む通常テキストはエラーなし", () => {
    expect(validateMemo("会議(10:00~)")).toBeNull();
  });
});
