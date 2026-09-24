/**
 * 아직 내용을 채우지 않은 화면의 공통 뼈대.
 *
 * 실제 기능(폼, 목록, 달력 등)을 붙일 때 이 컴포넌트 대신 각 페이지 내용을 작성한다.
 *
 * @param {object} props
 * @param {string} props.title 화면 제목
 * @param {string} [props.description] 화면 설명(선택)
 * @param {React.ReactNode} [props.children] 추가 내용(선택)
 */
function PagePlaceholder({ title, description, children }) {
  return (
    <section className="page">
      <h1 className="page__title">{title}</h1>
      {description && <p className="page__description">{description}</p>}
      {children}
    </section>
  )
}

export default PagePlaceholder
