import { apiFetch } from "@/lib/api-client";
import type { Subscription } from "./types";

/** GET /api/subscriptions/{id} — subscription-service SubscriptionController#get */
export function getSubscription(subscriptionId: number): Promise<Subscription> {
  return apiFetch<Subscription>(`/api/subscriptions/${subscriptionId}`);
}

/** POST /api/subscriptions/{id}/cancel — subscription-service SubscriptionController#cancel */
export function cancelSubscription(subscriptionId: number): Promise<void> {
  return apiFetch<void>(`/api/subscriptions/${subscriptionId}/cancel`, {
    method: "POST",
  });
}
