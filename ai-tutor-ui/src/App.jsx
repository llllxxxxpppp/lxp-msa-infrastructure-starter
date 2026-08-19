import { useCallback, useEffect, useState } from "react";

// [추가] PDF 관리 컴포넌트 import
import AiTutorDocumentManager from "./components/ai-tutor/AiTutorDocumentManager.jsx";
import AiTutorWidget from "./components/ai-tutor/AiTutorWidget.jsx";

export default function App() {
  const [accessToken, setAccessToken] = useState(
    () => localStorage.getItem("accessToken") ?? "",
  );
  // PDF 관리 화면에서 조회한 실제 담당 강좌
  const [courses, setCourses] = useState([]);

  useEffect(() => {
    const syncAccessToken = () => {
      setAccessToken(localStorage.getItem("accessToken") ?? "");
    };

    window.addEventListener("storage", syncAccessToken);
    window.addEventListener("focus", syncAccessToken);

    return () => {
      window.removeEventListener("storage", syncAccessToken);
      window.removeEventListener("focus", syncAccessToken);
    };
  }, []);

  const handleAuthenticationExpired = useCallback(() => {
    localStorage.removeItem("accessToken");
    setAccessToken("");
    setCourses([]);
  }, []);

  return (
    <>
      {/* [추가] 강사에게만 표시되는 PDF 관리 화면 */}
      {/* 조회한 담당 강좌를 App에 전달 */}
      <AiTutorDocumentManager
        accessToken={accessToken}
        onCoursesLoaded={setCourses}
      />

      {/* 기존 구름 모양 챗봇 */}
      <AiTutorWidget
        courses={courses}
        accessToken={accessToken}
        onAuthenticationExpired={handleAuthenticationExpired}
      />
    </>
  );
}
