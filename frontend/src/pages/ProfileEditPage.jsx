import { useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router'

import { updateMyProfile, uploadMyAvatar } from '@/api/users.js'
import Avatar from '@/components/Avatar.jsx'
import { useAuth } from '@/context/auth.js'

function ProfileEditPage() {
  const { user, applyProfile } = useAuth()
  const navigate = useNavigate()
  const fileInputRef = useRef(null)

  const [nickname, setNickname] = useState(user.nickname)
  const [bio, setBio] = useState(user.bio ?? '')
  const [error, setError] = useState(null)
  const [fieldErrors, setFieldErrors] = useState({})
  const [message, setMessage] = useState(null)
  const [saving, setSaving] = useState(false)
  const [uploading, setUploading] = useState(false)

  /** 서버가 준 필드별 오류를 상태에 담는다. (입력칸 아래에 표시) */
  const showErrors = (apiError) => {
    setError(apiError.message)

    const list = apiError.errors ?? []
    setFieldErrors(
      list.length > 0 ? Object.fromEntries(list.map((item) => [item.field, item.reason])) : {},
    )
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setError(null)
    setFieldErrors({})
    setMessage(null)
    setSaving(true)

    try {
      // 저장 결과를 전역 로그인 상태에도 반영한다. (헤더의 닉네임이 바로 바뀐다)
      applyProfile(await updateMyProfile({ nickname, bio }))
      setMessage('저장했습니다.')
    } catch (apiError) {
      showErrors(apiError)
    } finally {
      setSaving(false)
    }
  }

  const handleAvatarChange = async (event) => {
    const file = event.target.files?.[0]
    if (!file) {
      return
    }

    setError(null)
    setFieldErrors({})
    setMessage(null)
    setUploading(true)

    try {
      applyProfile(await uploadMyAvatar(file))
      setMessage('프로필 이미지를 바꿨습니다.')
    } catch (apiError) {
      showErrors(apiError)
    } finally {
      setUploading(false)
      // 같은 파일을 다시 선택할 수 있도록 input 을 비운다.
      if (fileInputRef.current) {
        fileInputRef.current.value = ''
      }
    }
  }

  return (
    <section className="page">
      <h1 className="page__title">프로필 편집</h1>
      <p className="page__description">핸들(@{user.handle})은 주소에 쓰이므로 바꿀 수 없습니다.</p>

      <div className="card">
        <h2 className="card__title">프로필 이미지</h2>
        <div className="profile__head">
          <Avatar size="large" user={user} />
          <div className="form__row">
            <label className="form__label" htmlFor="avatar">
              이미지 선택 (PNG·JPG·GIF·WEBP, 2MB 이하)
            </label>
            <input
              id="avatar"
              ref={fileInputRef}
              className="form__input"
              type="file"
              accept="image/png,image/jpeg,image/gif,image/webp"
              onChange={handleAvatarChange}
            />
            {uploading && <p className="text--muted">올리는 중...</p>}
            {fieldErrors.file && <p className="form__error">{fieldErrors.file}</p>}
          </div>
        </div>
      </div>

      <form className="form" onSubmit={handleSubmit}>
        <div className="form__row">
          <label className="form__label" htmlFor="nickname">
            닉네임 (20자 이하)
          </label>
          <input
            id="nickname"
            className="form__input"
            value={nickname}
            onChange={(event) => setNickname(event.target.value)}
          />
          {fieldErrors.nickname && <p className="form__error">{fieldErrors.nickname}</p>}
        </div>

        <div className="form__row">
          <label className="form__label" htmlFor="bio">
            자기소개 (300자 이하)
          </label>
          <textarea
            id="bio"
            className="form__input form__textarea"
            value={bio}
            onChange={(event) => setBio(event.target.value)}
          />
          {fieldErrors.bio && <p className="form__error">{fieldErrors.bio}</p>}
        </div>

        {error && <p className="text--error">{error}</p>}
        {message && <p className="text--success">{message}</p>}

        <div className="form__actions">
          <button className="button" type="submit" disabled={saving}>
            {saving ? '저장 중...' : '저장'}
          </button>
          <button className="button button--muted" type="button" onClick={() => navigate('/me')}>
            마이페이지로
          </button>
        </div>
      </form>

      <p className="text--muted">
        내 프로필은 <Link to={`/u/${user.handle}`}>공개 페이지</Link> 에서 볼 수 있습니다.
      </p>
    </section>
  )
}

export default ProfileEditPage
