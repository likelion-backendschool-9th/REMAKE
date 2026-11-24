import type { LegalBlock, LegalDocumentData } from "@/lib/legal/types";

import styles from "./LegalDocument.module.css";

/**
 * 피그마 common-이용 약관(5777-2768) 본문. 약관 드로워(Drawer) 안에 넣는다.
 * 조 제목(Remake/title·text/title) 아래 한 줄 띄우고 본문(Remake/title·text/body), 조 사이도 한 줄.
 * 번호 목록은 앞 문장에 바로 붙고 줄이 넘어가면 들여쓰기에 맞춘다. 표·글머리 목록은 피그마에 없어 같은 글자로 맞췄다.
 */
export function LegalDocument({ document }: { document: LegalDocumentData }) {
  return (
    <article className={styles.document}>
      {document.intro && (
        <div className={styles.blocks}>
          {document.intro.map((block, index) => (
            <Block key={index} block={block} />
          ))}
        </div>
      )}
      {document.sections.map((section) => (
        <section key={section.title} className={styles.section}>
          <h3 className={styles.title}>{section.title}</h3>
          <div className={styles.blocks}>
            {section.blocks.map((block, index) => (
              <Block key={index} block={block} />
            ))}
          </div>
        </section>
      ))}
      <p className={styles.effective}>시행일: {document.effectiveDate}</p>
    </article>
  );
}

function Block({ block }: { block: LegalBlock }) {
  switch (block.type) {
    case "paragraph":
      return <p>{block.text}</p>;
    case "ordered":
      return (
        <ol className={styles.ordered}>
          {block.items.map((item) => (
            <li key={item}>{item}</li>
          ))}
        </ol>
      );
    case "bullets":
      return (
        <ul className={styles.bullets}>
          {block.items.map((item) => (
            <li key={item}>{item}</li>
          ))}
        </ul>
      );
    case "table":
      return (
        <div className={styles.tableWrap}>
          <table className={styles.table}>
            <thead>
              <tr>
                {block.headers.map((header) => (
                  <th key={header} scope="col">
                    {header}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {block.rows.map((row) => (
                <tr key={row.join("|")}>
                  {row.map((cell, index) => (
                    <td key={index}>{cell}</td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      );
  }
}
