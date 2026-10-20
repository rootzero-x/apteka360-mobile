# Android qamrovi: desktop ekranlari → native Android (Jetpack Compose)

Manba: `apteka360-desktop/COVERAGE.md` (36 yo‘l) va `ui/app.js` (`NAV`, `ACCESS`).
Holat: **native** — Android ilovaning o‘z ekrani (Kotlin + Compose, Material 3), ma’lumot API’dan to‘g‘ridan-to‘g‘ri. Web’ga yo‘naltirish yo‘q.
Rollar: `ui/nav/Access.kt` — desktop `ACCESS` jadvali bilan bir xil; ruxsatsiz bo‘lim menyuda ko‘rinmaydi, deep link bilan ochilsa «yopiq» ekrani chiqadi. AUDITOR — barcha yozish tugmalari o‘chiq.

| # | Desktop ekran | Android ekran | Fayl (`ui/…`) | API | Holat |
|---|---|---|---|---|---|
| 1 | dashboard | `DashboardScreen` + FAB «Tez harakat» | screens/Dashboard.kt | /v1/dashboard, branches, approvals, suppliers, fomCashToday, aiEvents | native |
| 2 | branches | `BranchesScreen` | screens/Branches.kt | /v1/branches | native |
| 3 | branches/{id} | `BranchDetailScreen` | screens/Branches.kt | /v1/branches | native |
| 4 | expiry | `ExpiryScreen` | screens/Supply.kt | /v1/expiry/near, /v1/expiry/writeoffs | native |
| 5 | cash | `CashScreen` | screens/Cash.kt | /v1/desktop, fomCashToday, settings, desk.cashClose, desk.resolveConflict | native |
| 6 | expenses | `ExpensesScreen` | screens/Finance.kt | /v1/expenses, expenses.create/cancel, settings | native |
| 7 | cashflow | `CashflowScreen` | screens/Finance.kt | /v1/finance/cashflow | native |
| 8 | employees | `EmployeesScreen` (+ forma) | screens/Employees.kt | /v1/employees, employees.upsert/dismiss/restore | native |
| 9 | employee | `EmployeeDetailScreen` (7 tab) | screens/Employees.kt | /v1/employees, aiEvents, audit, audit.log | native |
| 10 | schedule | `ScheduleScreen` | screens/Schedule.kt | /v1/schedule, swaps, schedule.setCell/save, swaps.decide | native |
| 11 | attendance | `AttendanceScreen` | screens/Schedule.kt | /v1/employees, attendance.requestFix | native |
| 12 | live | `LiveBranchScreen` (30 s avto-yangilanish) | screens/Control.kt | /v1/branches, employees, cameras, aiEvents | native |
| 13 | payroll | `PayrollScreen` (+ payslip) | screens/Payroll.kt | /v1/payroll, payroll.recalc/review/lock/markPaid | native |
| 14 | payrollRules | `PayrollRulesScreen` | screens/Payroll.kt | /v1/payrollRules, payrollRules.* | native |
| 15 | suppliers | `SuppliersScreen` (+ to‘lov kalkulyatori) | screens/Supply.kt | /v1/suppliers, suppliers.upsert/createPayment | native |
| 16 | payables | `PayablesScreen` (+ firma paneli) | screens/Supply.kt | /v1/payables/* | native |
| 17 | doctors | `DoctorsScreen` | screens/Supply.kt | /v1/doctors, doctorSummaries, doctors.upsert/close | native |
| 18 | ai | `AiReviewScreen` | screens/Control.kt | /v1/aiEvents, reviewedToday, ai.* | native |
| 19 | videowall | `VideoWallScreen` (1/4/9, mini HLS) | screens/Control.kt | /v1/cameras, cameras.streamUrl | native |
| 20 | cameras | `CamerasAdminScreen` + `PlayerScreen` | screens/Control.kt, cameras/PlayerScreen.kt | /v1/cameras, cameras.create/update/setPrivacy/test/discover/streamUrl/talk | native |
| 21 | pnl | `PnlScreen` | screens/Finance.kt | /v1/finance/pnl | native |
| 22 | analytics | `AnalyticsScreen` (4 tab) | screens/Finance.kt | /v1/analytics/branches, staff, traffic, ai | native |
| 23 | forecast | `ForecastScreen` | screens/Finance.kt | /v1/finance/forecast | native |
| 24 | reports | `ReportsScreen` | screens/Finance.kt | /v1/reports, /v1/reports/{id} | native |
| 25 | tax | `TaxScreen` | screens/Finance.kt | /v1/soliq/status, summary, kassa, session | native |
| 26 | approvals | `ApprovalsScreen` (bottom sheet + biometriya) | screens/Approvals.kt | /v1/approvals, approvals.decide | native |
| 27 | notifications | `NotificationsScreen` | notifications/NotificationsScreen.kt | /v1/notifications, markRead/markAllRead | native |
| 28 | audit | `AuditScreen` | screens/Audit.kt | /v1/audit | native |
| 29 | fom | `FomScreen` | screens/System.kt | /v1/fom/agents/*, fomMap, fom.link | native |
| 30 | health | `HealthScreen` | screens/System.kt | /v1/edges, branches, fom/agents | native |
| 31 | users | `UsersScreen` + Telegram kartasi | screens/System.kt | /v1/admin/users, users.*, tg.* | native |
| 32 | roles | `RolesScreen` | screens/System.kt | /v1/roles, roles.update | native |
| 33 | modules | `ModulesScreen` | screens/System.kt | /v1/workspace, workspace/apply | native |
| 34 | aiSettings | `AiSettingsScreen` | screens/System.kt | /v1/ai/settings, models, test | native |
| 35 | settings | `SettingsScreen` (limitlar, idle, kategoriyalar, bildirish, xavfsizlik) | screens/System.kt | /v1/settings, settings.saveLimits, auth/sessions, logins | native |
| 36 | profile | `ProfileScreen` | profile/ProfileScreen.kt | /v1/me, profile.*, auth/sessions, auth/logins, tg.* | native |
| — | (mobil) | `HomeScreen` «Mening ishim» (xodim/mudir) | home/HomeScreen.kt | /v1/mobile, m.* | native |
| — | login | `AuthFlow` (login → OTP → yangi qurilma, parol tiklash) | auth/AuthScreens.kt | /v1/auth/* | native |

## Telefonga xos

| Imkoniyat | Qayerda |
|---|---|
| Pastki navigatsiya: rolga qarab 4 asosiy bo‘lim + «Yana» (guruhlangan); planshetda (≥ 600 dp) NavigationRail | ui/main/MainScaffold.kt, ui/nav/Access.kt |
| Ro‘yxat → batafsil (bottom sheet yoki to‘liq ekran), pull-to-refresh, skeleton, bo‘sh/xato holatlari | ui/components/Data.kt, Forms.kt |
| Biometrik tasdiq (BiometricPrompt): tasdiqlar (sozlamaga ko‘ra) va 10 mln+ summalar (majburiy: tasdiq, harajat, firmaga to‘lov) | ui/components/Biometric.kt |
| Kamera: to‘liq ekran landshaft, ovoz, bosib turib gapirish | ui/cameras/PlayerScreen.kt |
| FAB: harajat qo‘shish, kassa yopish (bosh sahifa); bo‘limlarda «+» | screens/Dashboard.kt va boshqalar |
| Deep link `apteka360://<bo‘lim>/<id>` va push bildirishnomadan ekranga o‘tish | MainActivity.kt, push/PushTokens.kt |
| Push (FCM) — kod tayyor, kalitsiz; Android 13+ ruxsat so‘rovi | push/, MainScaffold.kt |
| Haptic (AI tasdiq, tasdiqlash), predictive back, Material o‘tishlar, yorug‘/qorong‘i/tizim mavzusi | AndroidManifest.xml, MainScaffold.kt, theme/Theme.kt |

## Eslatma: biometriya va backend

`approvals.decide` da `biometricToken` faqat Telegram Mini App kanali uchun tekshiriladi; mobil sessiya backendda `web` kanali sifatida
qabul qilinadi. Shuning uchun Android’dagi biometrik tasdiq — telefondagi qo‘shimcha himoya (server tomonida token talab qilinmaydi).
