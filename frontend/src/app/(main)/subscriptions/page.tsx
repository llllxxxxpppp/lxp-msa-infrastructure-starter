"use client";

import { FormEvent, useState } from "react";
import * as subscriptionApi from "@/features/subscription/api";
import type { Subscription } from "@/features/subscription/types";
import { ApiError } from "@/types/api";
import { Card } from "@/components/ui/Card";
import { Chip } from "@/components/ui/Chip";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { ProgressBar } from "@/components/ui/ProgressBar";
import { MaterialIcon } from "@/components/ui/MaterialIcon";
import { Table, TableCell, TableHeaderCell, TableRow } from "@/components/ui/Table";

// MOCK: 결제수단/이용량/청구내역 API가 없다. 준비되면 실제 데이터로 교체한다.
const MOCK_PAYMENT_METHOD = { brand: "Visa", last4: "4242", expiry: "12/2025" };
const MOCK_USAGE = [
  { label: "이번 달 학습 시간", current: 32, max: 40, unit: "시간" },
  { label: "완료한 미션", current: 6, max: 10, unit: "개" },
];
const MOCK_BILLING_HISTORY = [
  { date: "2024-09-15", description: "구독 갱신", amount: "₩19,900", status: "결제완료" },
  { date: "2024-08-15", description: "구독 갱신", amount: "₩19,900", status: "결제완료" },
  { date: "2024-07-15", description: "구독 갱신", amount: "₩19,900", status: "결제완료" },
];

export default function SubscriptionsPage() {
  const [subscriptionId, setSubscriptionId] = useState("");
  const [subscription, setSubscription] = useState<Subscription | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [isCancelling, setIsCancelling] = useState(false);
  const [upgradeMessage, setUpgradeMessage] = useState<string | null>(null);

  async function handleSearch(event: FormEvent) {
    event.preventDefault();
    setIsLoading(true);
    setError(null);
    try {
      const result = await subscriptionApi.getSubscription(Number(subscriptionId));
      setSubscription(result);
    } catch (err) {
      setSubscription(null);
      setError(err instanceof ApiError ? err.message : "구독 정보를 불러오지 못했습니다.");
    } finally {
      setIsLoading(false);
    }
  }

  async function handleCancel() {
    if (!subscription) return;
    if (!window.confirm("구독을 취소하시겠습니까?")) return;
    setIsCancelling(true);
    try {
      await subscriptionApi.cancelSubscription(subscription.subscriptionId);
      setSubscription({ ...subscription, cancelledAt: new Date().toISOString() });
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "구독 취소에 실패했습니다.");
    } finally {
      setIsCancelling(false);
    }
  }

  return (
    <div className="gap-stack-lg flex flex-col">
      <div className="border-outline-variant pb-stack-md border-b">
        <h1 className="mb-base text-headline-lg text-primary">Subscription Management</h1>
        <p className="text-body-md text-slate-text">구독 상태와 결제 정보를 확인합니다.</p>
      </div>

      {/*
        백엔드 갭: subscription-service에 "내 구독 조회" 엔드포인트가 없다
        (SubscriptionInternalController#by-member는 /internal/**로 서비스 간 전용이라 게이트웨이 미노출).
        GET /api/subscriptions/me 같은 엔드포인트가 추가되면 이 조회 폼을 없애고 자동으로 불러오면 된다.
      */}
      <Card className="p-stack-md">
        <form onSubmit={handleSearch} className="flex items-end gap-2">
          <Input
            id="subscriptionId"
            label="구독 ID로 조회"
            value={subscriptionId}
            onChange={(e) => setSubscriptionId(e.target.value)}
            required
          />
          <Button type="submit" disabled={isLoading}>
            {isLoading ? "조회 중..." : "조회"}
          </Button>
        </form>
        {error && <p className="mt-stack-sm text-body-sm text-error-red">{error}</p>}
      </Card>

      {subscription && (
        <div className="gap-gutter grid grid-cols-1 lg:grid-cols-3">
          {/* Current Plan */}
          <Card className="p-stack-lg relative flex flex-col justify-between overflow-hidden lg:col-span-2">
            <div className="mb-stack-md z-10 flex items-start justify-between">
              <div>
                <span className="mb-stack-sm bg-primary-fixed text-label-sm text-on-primary-fixed inline-block rounded-full px-3 py-1">
                  Current Plan
                </span>
                <h2 className="text-headline-md text-primary">
                  구독 #{subscription.subscriptionId}
                </h2>
                <p className="mt-base text-body-sm text-slate-text">
                  {subscription.generation}세대 구독 · 회원 #{subscription.memberId}
                </p>
              </div>
              <div className="text-right">
                {subscription.cancelledAt ? (
                  <div className="text-label-sm text-error-red mt-1 flex items-center justify-end gap-1">
                    <MaterialIcon name="cancel" className="text-[16px]" />
                    취소됨
                  </div>
                ) : (
                  <div className="text-label-sm text-success-green mt-1 flex items-center justify-end gap-1">
                    <MaterialIcon name="check_circle" className="text-[16px]" />
                    Active
                  </div>
                )}
              </div>
            </div>
            <div className="gap-stack-md border-surface-container pt-stack-md z-10 flex flex-col justify-between border-t sm:flex-row sm:items-center">
              <div className="flex flex-col gap-1">
                <span className="text-label-sm text-slate-text tracking-wider uppercase">
                  만료일
                </span>
                <span className="text-label-md text-primary">
                  {new Date(subscription.validUntil).toLocaleDateString("ko-KR")}
                </span>
              </div>
              <div className="gap-stack-sm flex">
                <Button
                  variant="secondary"
                  disabled={!!subscription.cancelledAt || isCancelling}
                  onClick={handleCancel}
                >
                  {isCancelling ? "취소 중..." : "Cancel Plan"}
                </Button>
                <Button onClick={() => setUpgradeMessage("업그레이드 기능은 준비 중입니다.")}>
                  Upgrade Plan
                </Button>
              </div>
            </div>
            {upgradeMessage && (
              <p className="mt-stack-sm text-body-sm text-slate-text z-10">{upgradeMessage}</p>
            )}
          </Card>

          {/* Payment Method (MOCK) */}
          <Card className="p-stack-lg flex flex-col">
            <div className="mb-stack-md flex items-center justify-between">
              <h3 className="text-headline-sm text-primary">Payment Method</h3>
            </div>
            <div className="mb-stack-md border-surface-container bg-surface-bright py-stack-md flex flex-1 flex-col items-center justify-center rounded-lg border">
              <MaterialIcon name="credit_card" className="text-slate-text mb-2 text-[48px]" />
              <div className="text-label-md text-primary">
                {MOCK_PAYMENT_METHOD.brand} ending in **** {MOCK_PAYMENT_METHOD.last4}
              </div>
              <div className="text-label-sm text-slate-text mt-1">
                Expires {MOCK_PAYMENT_METHOD.expiry}
              </div>
            </div>
            <Button variant="secondary" className="w-full" disabled>
              Add New Method
            </Button>
          </Card>

          {/* Plan Usage (MOCK) */}
          <Card className="p-stack-lg lg:col-span-3">
            <h3 className="mb-stack-md text-headline-sm text-primary">Plan Usage</h3>
            <div className="gap-stack-md flex flex-col">
              {MOCK_USAGE.map((usage) => (
                <div key={usage.label}>
                  <div className="mb-2 flex items-end justify-between">
                    <span className="text-label-md text-primary">{usage.label}</span>
                    <span className="text-label-sm text-slate-text">
                      {usage.current} / {usage.max}
                      {usage.unit}
                    </span>
                  </div>
                  <ProgressBar value={(usage.current / usage.max) * 100} />
                </div>
              ))}
            </div>
          </Card>

          {/* Billing History (MOCK) */}
          <Card className="overflow-hidden lg:col-span-3">
            <div className="border-surface-container bg-surface p-stack-md flex items-center justify-between border-b">
              <h3 className="text-headline-sm text-primary">Billing History</h3>
            </div>
            <Table>
              <thead>
                <tr>
                  <TableHeaderCell>Date</TableHeaderCell>
                  <TableHeaderCell>Description</TableHeaderCell>
                  <TableHeaderCell>Amount</TableHeaderCell>
                  <TableHeaderCell>Status</TableHeaderCell>
                </tr>
              </thead>
              <tbody>
                {MOCK_BILLING_HISTORY.map((row) => (
                  <TableRow key={row.date}>
                    <TableCell>{row.date}</TableCell>
                    <TableCell>{row.description}</TableCell>
                    <TableCell>{row.amount}</TableCell>
                    <TableCell>
                      <Chip tone="success">{row.status}</Chip>
                    </TableCell>
                  </TableRow>
                ))}
              </tbody>
            </Table>
          </Card>
        </div>
      )}
    </div>
  );
}
