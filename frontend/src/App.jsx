import { useCallback, useEffect, useState } from 'react'

import { fetchHello, sendEcho } from '@/api/greeting'

import './App.css'

const DEFAULT_NAME = 'godsse'
const DEFAULT_MESSAGE = 'React + Spring Boot 연동 확인'

function App() {
  const [hello, setHello] = useState(null)
  const [helloError, setHelloError] = useState(null)
  const [loading, setLoading] = useState(true)

  const [name, setName] = useState(DEFAULT_NAME)
  const [message, setMessage] = useState(DEFAULT_MESSAGE)
  const [echo, setEcho] = useState(null)
  const [echoError, setEchoError] = useState(null)

  // setState 는 effect 본문이 아니라 .then/.catch 콜백(비동기)에서만 호출한다.
  const showHello = useCallback((data) => {
    setHello(data)
    setHelloError(null)
    setLoading(false)
  }, [])

  const showHelloError = useCallback((message) => {
    setHello(null)
    setHelloError(message)
    setLoading(false)
  }, [])

  // 최초 1회 조회 (StrictMode 의 effect 중복 실행에도 안전하도록 ignore 플래그 사용)
  useEffect(() => {
    let ignore = false

    fetchHello()
      .then((data) => {
        if (!ignore) showHello(data)
      })
      .catch((error) => {
        if (!ignore) showHelloError(error.message)
      })

    return () => {
      ignore = true
    }
  }, [showHello, showHelloError])

  const handleReload = () => {
    setLoading(true)
    setHelloError(null)
    fetchHello()
      .then(showHello)
      .catch((error) => showHelloError(error.message))
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setEcho(null)
    setEchoError(null)
    try {
      setEcho(await sendEcho({ name, message }))
    } catch (error) {
      setEchoError(error.message)
    }
  }

  return (
    <main className="app">
      <h1 className="app__title">godsse</h1>
      <p className="app__subtitle">React (Vite) + Java Spring Boot</p>

      <section className="card">
        <h2 className="card__title">GET /api/hello</h2>
        {loading && <p className="text--muted">백엔드 응답을 기다리는 중...</p>}
        {helloError && (
          <p className="text--error">
            {helloError}
            <br />
            <span className="text--muted">
              backend 를 먼저 실행했는지 확인해 주세요. (mvnw spring-boot:run)
            </span>
          </p>
        )}
        {hello && (
          <dl className="result">
            <dt>message</dt>
            <dd>{hello.message}</dd>
            <dt>timestamp</dt>
            <dd>{hello.timestamp}</dd>
          </dl>
        )}
        <button type="button" className="button" onClick={handleReload}>
          다시 요청
        </button>
      </section>

      <section className="card">
        <h2 className="card__title">POST /api/hello/echo</h2>
        <form className="form" onSubmit={handleSubmit}>
          <label className="form__label" htmlFor="name">
            name
          </label>
          <input
            id="name"
            className="form__input"
            value={name}
            onChange={(event) => setName(event.target.value)}
          />
          <label className="form__label" htmlFor="message">
            message
          </label>
          <input
            id="message"
            className="form__input"
            value={message}
            onChange={(event) => setMessage(event.target.value)}
          />
          <button type="submit" className="button">
            전송
          </button>
        </form>
        {echoError && <p className="text--error">{echoError}</p>}
        {echo && (
          <dl className="result">
            <dt>name</dt>
            <dd>{echo.name}</dd>
            <dt>message</dt>
            <dd>{echo.message}</dd>
            <dt>length</dt>
            <dd>{echo.length}</dd>
          </dl>
        )}
      </section>
    </main>
  )
}

export default App
