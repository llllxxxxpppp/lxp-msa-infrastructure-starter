import { redirect } from "next/navigation";

/** 시작 화면은 랜딩 페이지 없이 바로 로그인 화면(/login)이다. */
export default function Home() {
  redirect("/login");
}
