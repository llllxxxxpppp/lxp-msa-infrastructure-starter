import AiTutorWidget from "./components/ai-tutor/AiTutorWidget.jsx";

const courses = [
  {
    id: 1,
    title: "테스트 강좌",
  },
];

export default function App() {
  // 실제 프로젝트의 로그인 상태와 연결할 부분
  const accessToken = localStorage.getItem("accessToken") ?? "";

  return (
    <AiTutorWidget
      courses={courses}
      accessToken={accessToken}
    />
  );
}