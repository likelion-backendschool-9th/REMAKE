/** className 합치기. false·null·undefined·빈 문자열은 뺀다. */
export function cx(...names: Array<string | false | null | undefined>): string {
  return names.filter(Boolean).join(" ");
}
