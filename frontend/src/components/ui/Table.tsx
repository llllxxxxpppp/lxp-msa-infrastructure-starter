import { HTMLAttributes, TdHTMLAttributes, ThHTMLAttributes } from "react";

/**
 * design/subscription의 Billing History 테이블 스타일을 그대로 옮긴 얇은 래퍼들.
 * DESIGN.md Lists: 1px 구분선 + hover 시 옅은 Slate 틴트.
 */
export function Table(props: HTMLAttributes<HTMLTableElement>) {
  return (
    <div className="overflow-x-auto">
      <table className="w-full border-collapse text-left" {...props} />
    </div>
  );
}

export function TableHeaderCell({
  className = "",
  ...props
}: ThHTMLAttributes<HTMLTableCellElement>) {
  return (
    <th
      className={`border-outline-variant px-stack-md text-label-sm text-slate-text border-b py-3 font-semibold tracking-wider uppercase ${className}`}
      {...props}
    />
  );
}

export function TableRow({ className = "", ...props }: HTMLAttributes<HTMLTableRowElement>) {
  return (
    <tr
      className={`border-outline-variant hover:bg-surface-container-low border-b last:border-0 ${className}`}
      {...props}
    />
  );
}

export function TableCell({ className = "", ...props }: TdHTMLAttributes<HTMLTableCellElement>) {
  return <td className={`px-stack-md text-body-sm text-on-surface py-3 ${className}`} {...props} />;
}
