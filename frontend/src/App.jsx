import { Route, Routes } from 'react-router'

import ProtectedRoute from '@/components/ProtectedRoute.jsx'
import { AuthProvider } from '@/context/AuthProvider.jsx'
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
 *
 * AuthProvider 는 로그인 상태를 앱 전체에 공급하고,
 * ProtectedRoute 는 로그인이 필요한 화면을 감싼다(비로그인 시 /login 으로 이동).
 */
function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route element={<AppLayout />}>
          <Route index element={<HomePage />} />
          <Route path="login" element={<LoginPage />} />
          <Route path="signup" element={<SignupPage />} />
          <Route path="search" element={<SearchPage />} />
          <Route path="u/:handle" element={<ProfilePage />} />
          <Route path="u/:handle/received" element={<ReceivedReviewListPage />} />
          <Route path="u/:handle/written" element={<WrittenReviewListPage />} />
          <Route path="reviews/:id" element={<ReviewDetailPage />} />
          <Route
            path="reviews/new"
            element={
              <ProtectedRoute>
                <ReviewNewPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="me"
            element={
              <ProtectedRoute>
                <MyPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="me/edit"
            element={
              <ProtectedRoute>
                <ProfileEditPage />
              </ProtectedRoute>
            }
          />
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Routes>
    </AuthProvider>
  )
}

export default App
