/**
 * subscription-service의 응답 DTO와 1:1로 맞춘 타입.
 * (subscription-service/.../application/dto/response/SubscriptionResponse.java)
 * 날짜 필드는 서버가 ISO-8601 문자열(OffsetDateTime)로 내려주므로 string으로 받는다.
 */
export interface Subscription {
  subscriptionId: number;
  memberId: number;
  parentId: number | null;
  generation: number;
  subscriptionStartAt: string;
  validUntil: string;
  activatedAt: string | null;
  suspendedAt: string | null;
  cancelledAt: string | null;
  createdAt: string;
}
