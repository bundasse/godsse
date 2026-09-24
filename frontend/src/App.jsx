import { Route, Routes } from 'react-router'

import AppLayout from '@/layouts/AppLayout.jsx'
import HomePage from '@/pages/HomePage.jsx'
import LoginPage from '@/pages/LoginPage.jsx'
import MyPage from '@/pages/MyPage.jsx'
import NotFoundPage from '@/pages/NotFoundPage.jsx'
import ProfileEditPage from '@/pages/ProfileEditPage.jsx'
import ProfilePage from '@/pages/ProfilePage.jsx'
import ReceivedReviewListPage from '@/pages/ReceivedReviewListPage.jsx'
import ReviewDetailPage from '@/pages/ReviewDetailPage.jsx'
import ReviewNewPage from '@/pages/ReviewNewPage.jsx'
import SearchPage from '@/pages/SearchPage.jsx'
import SignupPage from '@/pages/SignupPage.jsx'
import WrittenReviewListPage from '@/pages/WrittenReviewListPage.jsx'

import './App.css'

/**
 * 전체 화면(라우트) 표.
 *
 * AppLayout 안쪽에 각 페이지가 렌더링된다(헤더/모바일 탭은 공통).
 * React Router 의 <Route> 는 Vue Router 의 routes 배열과 같은 역할이다.
 */
function App() {
  return (
    <Routes>
      <Route element={<AppLayout />}>
        <Route index element={<HomePage />} />
        <Route path="login" element={<LoginPage />} />
        <Route path="signup" element={<SignupPage />} />
        <Route path="search" element={<SearchPage />} />
        <Route path="u/:handle" element={<ProfilePage />} />
        <Route path="u/:handle/received" element={<ReceivedReviewListPage />} />
        <Route path="u/:handle/written" element={<WrittenReviewListPage />} />
        <Route path="reviews/new" element={<ReviewNewPage />} />
        <Route path="reviews/:id" element={<ReviewDetailPage />} />
        <Route path="me" element={<MyPage />} />
        <Route path="me/edit" element={<ProfileEditPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}

export default App
