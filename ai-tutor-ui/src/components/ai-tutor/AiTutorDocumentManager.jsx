import { useEffect, useState } from "react";
import "./AiTutorDocumentManager.css";

const authorization = (accessToken) => ({
  Authorization: `Bearer ${accessToken}`,
});

// 선택한 강좌의 PDF 목록을 조회한다.
async function fetchDocuments(courseId, accessToken) {
  const response = await fetch(
    `/api/ai/courses/${courseId}/documents`,
    {
      headers: authorization(accessToken),
    },
  );

  if (!response.ok) {
    throw new Error();
  }

  return response.json();
}

export default function AiTutorDocumentManager({
  accessToken,
  onCoursesLoaded,
}) {
  const [courses, setCourses] = useState([]);
  const [courseId, setCourseId] = useState("");
  const [documents, setDocuments] = useState([]);
  const [visible, setVisible] = useState(false);
  const [message, setMessage] = useState("");

  // 로그인한 강사의 담당 강좌를 조회한다.
  useEffect(() => {
    if (!accessToken) {
      return;
    }

    async function loadCourses() {
      try {
        const response = await fetch(
          "/api/courses/instructor/me",
          {
            headers: authorization(accessToken),
          },
        );

        // 강사가 아니면 PDF 관리 화면을 표시하지 않는다.
        if (response.status === 403) {
          return;
        }

        if (!response.ok) {
          throw new Error();
        }

        const data = await response.json();

        setVisible(true);
        setCourses(data);
        // 챗봇에서도 같은 강좌 ID를 사용하도록 App에 전달
        onCoursesLoaded(
          data.map((course) => ({
            id: course.courseId,
            title: course.title,
          })),
        );
        setCourseId(data[0]?.courseId ?? "");
      } catch {
        setVisible(true);
        setMessage("담당 강좌를 불러오지 못했습니다.");
      }
    }

    loadCourses();
  }, [accessToken, onCoursesLoaded]);

  // 강좌가 변경되면 해당 강좌의 PDF 목록을 조회한다.
  useEffect(() => {
    if (!courseId) {
      setDocuments([]);
      return;
    }

    fetchDocuments(courseId, accessToken)
      .then(setDocuments)
      .catch(() => setMessage("PDF 목록을 불러오지 못했습니다."));
  }, [courseId, accessToken]);

  async function uploadDocument(event) {
    event.preventDefault();

    const form = event.currentTarget;
    const formData = new FormData(form);

    setMessage("");

    const response = await fetch(
      `/api/ai/courses/${courseId}/documents`,
      {
        method: "POST",
        headers: authorization(accessToken),
        body: formData,
      },
    );

    if (!response.ok) {
      setMessage("PDF 업로드에 실패했습니다.");
      return;
    }

    form.reset();
    setDocuments(await fetchDocuments(courseId, accessToken));
    setMessage("PDF를 등록했습니다.");
  }

  async function deleteDocument(documentId) {
    setMessage("");

    const response = await fetch(
      `/api/ai/courses/${courseId}/documents/${documentId}`,
      {
        method: "DELETE",
        headers: authorization(accessToken),
      },
    );

    if (!response.ok) {
      setMessage("PDF 삭제에 실패했습니다.");
      return;
    }

    setDocuments(await fetchDocuments(courseId, accessToken));
    setMessage("PDF를 삭제했습니다.");
  }

  if (!visible) {
    return null;
  }

  return (
    <section className="ai-document-manager">
      <h2>강의 자료 관리</h2>

      {courses.length === 0 ? (
        <p>담당 강좌가 없습니다.</p>
      ) : (
        <>
          <select
            value={courseId}
            onChange={(event) => setCourseId(event.target.value)}
          >
            {courses.map((course) => (
              <option
                key={course.courseId}
                value={course.courseId}
              >
                {course.title}
              </option>
            ))}
          </select>

          <form onSubmit={uploadDocument}>
            <input
              name="file"
              type="file"
              accept="application/pdf"
              required
            />
            <button type="submit">PDF 등록</button>
          </form>

          <ul>
            {documents.map((document) => (
              <li key={document.document_id}>
                <span>{document.filename}</span>
                <button
                  type="button"
                  onClick={() =>
                    deleteDocument(document.document_id)
                  }
                >
                  삭제
                </button>
              </li>
            ))}
          </ul>

          {documents.length === 0 && (
            <p>등록된 PDF가 없습니다.</p>
          )}
        </>
      )}

      {message && <p className="ai-document-message">{message}</p>}
    </section>
  );
}
