export const MEMO_MAX_LENGTH = 20;

const MEMO_INVALID_PATTERN = /[\n\r]|<[^>]*>|\p{Emoji_Presentation}|\p{Extended_Pictographic}/u;

export function validateMemo(value: string): string | null {
  if (value.length > MEMO_MAX_LENGTH) {
    return `メモは${MEMO_MAX_LENGTH}文字以内で入力してください`;
  }
  if (MEMO_INVALID_PATTERN.test(value)) {
    return "改行・絵文字・HTMLタグは使用できません";
  }
  return null;
}
