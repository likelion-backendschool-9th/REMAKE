# Payment Terms

> English translation of [`../developer/payment-terms.md`](../developer/payment-terms.md). The Korean document is the original and the text users see (subscription management p1.3.4 "결제 약관 동의 · 자세히 보기" terms drawer). When either changes, update both and `frontend/lib/legal/payment-terms.ts`.

These terms set the payment, cancellation, and refunds of the Remake premium subscription provided by Junho Lee (the "Operator"). Anything not covered here follows the Remake Terms of Service.

## 1. Product

- Name: REMAKE Premium Membership
- Price: 9,900 KRW per month (VAT included)
- Benefits: up to 4 AI recipe generations per day; 4 AI meal-plan generations per month, 7 days each

## 2. Payment and auto-renewal

1. Checking "결제 약관 동의" and subscribing registers a card for auto-billing in the Toss Payments window and charges the first month at once. The subscription starts as soon as the payment succeeds.
2. The subscription renews automatically every month. On the next billing date (start date + 1 month; the last day of the month when that date does not exist) the same amount is charged to the registered card.
3. The next billing date and amount can be checked at any time in Settings > Subscription.
4. If an auto-payment fails, the subscription ends at once and the member becomes a basic member. To subscribe again, a card must be registered again.

## 3. Cancellation

1. Users can cancel at any time in Settings > Subscription > "구독 해지 하기".
2. After cancelling, benefits stay through the paid period (the day before the next billing date); no further charge is made from the next billing date, and the member becomes a basic member.
3. A user cannot subscribe again while a cancellation is scheduled; a new subscription is possible after it ends.
4. When the subscription ends, basic limits apply; meal-plan generations already used that month still count.

## 4. Withdrawal of subscription and refunds

1. A full refund is available within 7 days of payment if no premium benefit has been used since that payment: meal-plan generation count is 0 and AI recipe generations above the BASIC daily limit are 0 (#228).
2. Request the refund from the in-app refund page. On success the subscription ends (ENDED, plan BASIC); payment records are kept.
3. Otherwise the user can only cancel; there is no refund for the paid period.

## 5. Account deletion

Deleting the account ends the subscription at once with no further charges. There is no refund for the remaining period.

## 6. Payment information

Toss Payments handles card information; Remake does not store card numbers. Remake keeps only the auto-billing key, encrypted, and deletes it when the subscription ends.

## 7. Price changes

If the price or benefits change, users are notified in the Service 30 days before the next billing date and may cancel if they do not agree.

## 8. Demo payment notice

Remake's subscription payment currently runs in the Toss Payments test environment as a demo payment; no real card charge is made.

## 9. Contact

Payment and subscription inquiries: sjsk3358@gmail.com

- Effective date: December 1, 2026
