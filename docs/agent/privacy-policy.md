# Privacy Policy

> English translation of [`../developer/privacy-policy.md`](../developer/privacy-policy.md). The Korean document is the original and the text users see (login screen privacy policy drawer). When either changes, update both and `frontend/lib/legal/privacy-policy.ts`.

Remake (the "Service") keeps this privacy policy under the Personal Information Protection Act to protect users' personal information and to handle related complaints smoothly.

- Operator: Junho Lee
- Service URL: https://remake.newlecture.com/

## Article 1. Personal information processed and purposes

The Service processes only the minimum personal information needed for the purposes below. It does not collect health information such as diseases, or names, phone numbers, or dates of birth.

| Category | Items | Purpose |
| --- | --- | --- |
| Sign-up and login (required) | Social login provider (Kakao, Naver, Google), the member's unique ID from the provider, email address (Kakao, Google) | Identify members, keep login, send the verification code for account deletion |
| Service settings (required) | Household size, ingredients to exclude from recipes (chosen from 19 allergens), ingredient supplement and notification settings, last chosen meal purpose | Used in recipe and meal-plan generation |
| Entered while using the Service | Fridge ingredients (name, category, storage, amount, expiry date, price), used/discarded records, recipes, meal plans, cart, saved recipes, notifications | Fridge management, AI recipe and meal-plan generation, expiry notifications |
| Receipt registration (optional feature) | Receipt photo; store (branch), purchase date and time, receipt number, food items, and amounts read from it | Automatic fridge registration, monthly spending and patterns, duplicate receipt check |
| Sharing space (optional feature) | Title and cooking photo of shared recipes, report history | Posting shared recipes (author info is not shown to other users), handling reports, weekly popular recipe rewards |
| Premium subscription (optional feature) | Payment records (order number, amount, date and time, result), auto-billing key issued by Toss Payments (stored encrypted) | Subscription payment, monthly renewal, cancellation |
| Generated automatically | Usage records (which feature was used on which screen), environment (app or web, app version), login cookies | Usage statistics for improvement, error checks, abuse prevention |

- Payment method information such as card numbers is not received or stored by the Service; the payment agent (Toss Payments) handles it.
- Usage records never contain email, name, social unique ID, allergen names, typed ingredient names, recipe content, or receipt photos.

## Article 2. Retention and destruction

When a user deletes the account, data is destroyed without delay except as listed below. **Subscription payment records (`subscription_payments`) are not an exception — they are destroyed on account deletion** (#339). While the account exists they may be kept for billing. The auto-billing key is deleted when the subscription ends (payment terms).

| Item | Retention |
| --- | --- |
| Member info (social unique ID, email) | Until account deletion; removed at once on deletion |
| Settings, fridge ingredients, usage logs, meal plans, cart | Until account deletion |
| Receipts (including photos) | 6 months from the purchase date; deleted automatically every early morning after that. Deleted at once when the user deletes them |
| Receipt analysis results | Not stored before registration |
| Notifications | 30 days |
| Account deletion verification codes | One day after sending (valid for 5 minutes) |
| Login session data | Deleted on logout or after 14 days |
| Usage records | 180 days. After account deletion only the member number remains, not linked to email or social ID |
| Shared recipes and cooking photos | Until the author deletes them. If not deleted before account deletion, they stay as anonymous posts not linked to the author |

- Destruction method: electronic files are deleted so they cannot be recovered.

## Article 3. Provision to third parties

The Service does not provide users' personal information to third parties, except when required by law.

## Article 4. Outsourcing and overseas transfer

| Recipient | Task | Overseas transfer |
| --- | --- | --- |
| Toss Payments Co., Ltd. | Subscription payment, card auto-billing registration | None |

## Article 5. User rights

- Users can change household size, excluded ingredients, and notification settings in Settings at any time, and delete fridge ingredients, receipts, and saved recipes.
- Users can delete the account at any time in Settings > Delete account (identity check by email code for Kakao and Google, by re-login for Naver).
- For other requests to view, correct, delete, or suspend processing of personal information, contact the address below; requests are handled without delay.

## Article 6. Cookies and automatic collection

- Two cookies keep the user logged in (access 30 minutes, refresh 14 days). They are not used for cross-site tracking or advertising.
- The last used social provider is remembered on the user's device (browser storage) for the "recent login" label. It is not sent to the server.
- No external analytics tools are used. Usage records are stored only on the Service's servers.

## Article 7. Security measures

- Login tokens and verification codes are stored as hashes, not in plain text.
- The auto-billing key is encrypted and kept only on the server; the payment secret key is never sent to the app.
- Traffic is encrypted with HTTPS.
- Access to personal information is limited to the Operator.

## Article 8. Privacy officer and contact

| Item | Details |
| --- | --- |
| Privacy officer | Junho Lee |
| Contact | sjsk3358@gmail.com |

Users can also consult or report privacy violations to:

- Personal Information Infringement Report Center: privacy.kisa.or.kr / 118 (no area code)
- Personal Information Dispute Mediation Committee: www.kopico.go.kr / 1833-6972
- Supreme Prosecutors' Office: www.spo.go.kr / 1301 (no area code)
- Korean National Police Agency: ecrm.police.go.kr / 182 (no area code)

## Article 9. Changes to this policy

Changes are announced in the Service 7 days before they take effect.

- Effective date: December 1, 2026
