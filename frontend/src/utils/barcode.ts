/**
 * 道具条码校验（与后端 BarcodeValidator 规则保持一致）。
 * 支持 EAN-13 / EAN-8 / UPC-A（纯数字，含校验位）、Code 39、Code 128。
 */

const MAX_LENGTH = 50
const CODE39_PATTERN = /^[0-9A-Z\-. $/+%]+$/

export function isValidBarcode(raw: string | null | undefined): boolean {
  if (!raw) return false
  const code = raw.trim()
  if (!code || code.length > MAX_LENGTH) return false
  if (/^\d+$/.test(code)) {
    // 纯数字码必须是带合法校验位的 EAN-13 / EAN-8 / UPC-A
    return isValidEan(code, 13) || isValidEan(code, 8) || isValidEan(code, 12)
  }
  if (CODE39_PATTERN.test(code) && code.length >= 3 && code.length <= 43) return true
  // Code 128：可打印 ASCII
  return code.length >= 4 && code.length <= 48 && /^[\x20-\x7E]+$/.test(code)
}

/**
 * 校验指定长度的 EAN/UPC 校验位。
 * 总长度为奇数时最左位权重为 1，偶数时最左位权重为 3，交替加权。
 */
function isValidEan(code: string, length: number): boolean {
  if (code.length !== length) return false
  let sum = 0
  for (let i = 0; i < length - 1; i++) {
    const digit = code.charCodeAt(i) - 48
    const weight = (i % 2 === 0) === (length % 2 === 1) ? 1 : 3
    sum += digit * weight
  }
  const check = (10 - (sum % 10)) % 10
  return check === code.charCodeAt(length - 1) - 48
}
