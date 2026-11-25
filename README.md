# Northbridge Mobile for Android

Retail mobile banking app for Northbridge Federal Bank customers. Owned by Mobile Engineering (Digital Channels).

Customers can:

- sign in with their Online ID and passcode
- see an accounts overview (available / ledger balances, holds)
- browse transaction history (last 90 days, paged) and search it
- transfer money between their own deposit accounts
- pay bills to saved payees and send person-to-person (P2P) payments through the payments hub

## Tech stack

| Area | Version |
|---|---|
| Language | Java 8 (most code) + Kotlin 1.4 (utilities, mock backend) |
| Build | Gradle 6.9 (Groovy DSL), Android Gradle Plugin 4.2.2 |
| SDK | minSdk 23, target/compile SDK 30 |
| Networking | Retrofit 2.6, OkHttp 3.12, Gson 2.8 |
| UI | AndroidX AppCompat 1.2, Material 1.2, RecyclerView, SwipeRefreshLayout |
| Tests | JUnit 4.12, Robolectric 4.5, Mockito 2.28, JaCoCo 0.8.7 |

## Setup

Prerequisites:

- JDK 8 (AGP 4.2 also runs on JDK 11; newer JDKs are not supported by this AGP)
- Android SDK with `platforms;android-30` and `build-tools;30.0.3`
- An emulator image (API 23+), e.g. `system-images;android-30;google_apis;x86_64`

```bash
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
export ANDROID_SDK_ROOT=$HOME/android-sdk      # or write sdk.dir to local.properties
./gradlew assembleMockDebug
```

Dependencies resolve from Google Maven and a Maven Central mirror
(`maven-central.storage-download.googleapis.com`) before `mavenCentral()`, because direct Central
access is rate limited from build agents.

## Build flavours

| Flavour | Backend | Application ID |
|---|---|---|
| `mock` | In-process stub of core-banking and the payments hub (`app/src/mock`). No network needed. | `com.northbridge.mobile.mock` |
| `live` | Local core-banking on `10.0.2.2:8080` and payments hub on `10.0.2.2:8081` (the host machine as seen from the emulator). | `com.northbridge.mobile` |

Build types: `debug`, `uat` (debuggable, `.uat` suffix, points at the same URLs as debug) and `release`.

## Run

```bash
emulator -avd <your-avd> &
adb wait-for-device
./gradlew installMockDebug
adb shell am start -n com.northbridge.mobile.mock/com.northbridge.mobile.ui.login.LoginActivity
```

Sign in with a seeded customer from the core platform API contract:

| Online ID | Passcode | CIF | Accounts |
|---|---|---|---|
| `dana.whitfield` | `Welcome1!` | 100000001 | Checking ********3455, Savings ********3463 |
| `marcus.oyelaran` | `Welcome1!` | 100000002 | Checking ********8218, Savings ********8226 |

To run against the real services, start `core-banking` (:8080) and the payments hub (:8081) from the
Northbridge Core Platform repository on the host, then `./gradlew installLiveDebug`.

## Architecture

```
ui/                Activities + RecyclerView adapters (one Activity per screen)
  login/           LoginActivity            -> GET  customers/me
  accounts/        AccountsActivity         -> GET  customers/{cif}/accounts
  transactions/    TransactionHistoryActivity -> GET accounts/{n}/transactions, GET transactions/search
  transfer/        TransferActivity         -> POST transfers
  payments/        PaymentsActivity, BillPayActivity, SendMoneyActivity -> payments hub
  help/            HelpActivity (bundled help centre in a WebView)
api/               Retrofit interfaces, ApiClient (OkHttp setup), AuthInterceptor, ApiErrors
model/             Gson DTOs matching the core platform JSON contract
session/           SessionManager (credentials, customer profile, remember-me)
util/              Money.kt, AccountNumbers.kt, BankDates.java
src/mock/          MockBank.kt + MockBackendInterceptor.kt (offline backend)
src/live/          BackendInterceptors with no stub
```

`ApiClient` builds one OkHttp client shared by two Retrofit instances (core-banking and payments).
Each flavour supplies `BackendInterceptors.install(builder)`: the `mock` flavour adds
`MockBackendInterceptor`, which answers requests for the two base URLs from in-memory state seeded
with the contract's customers, accounts and payees; the `live` flavour adds nothing.

## How the app talks to core-banking

The contract is `docs/API.md` in the Northbridge Core Platform repository.

- **Auth**: HTTP Basic against the core-banking user store on every request (`AuthInterceptor`).
  A `401` ends the session and returns the customer to the login screen. Customers (`CUSTOMER` role)
  can only see their own CIF and accounts; the server enforces this and the mock does too.
- **Login**: `GET /api/v1/customers/me` returns the signed-in customer's profile and CIF.
- **Accounts**: `GET /api/v1/customers/{cif}/accounts`.
- **History**: `GET /api/v1/accounts/{accountNumber}/transactions?from&to&page&size`;
  search uses `GET /api/v1/transactions/search?q&accountNumber`.
- **Transfers**: `POST /api/v1/transfers` with an `idempotencyKey` (UUID per confirmation).
- **Payments hub** (`/api/v1/payments`): `GET payees`, `POST bill`, `POST p2p`, `GET history`.
- Money is sent and received as two-decimal strings (`"125.40"`), dates as `yyyy-MM-dd`, and account
  numbers are 12-digit DDAs that are masked to the last four digits in the UI.

Every request also carries `X-NB-Client-Id`, `X-NB-Client-Key` and `X-NB-App-Version` for the API gateway.

## Tests and coverage

```bash
./gradlew testMockDebugUnitTest      # JUnit + Robolectric
./gradlew jacocoTestReport           # app/build/reports/jacoco/jacocoTestReport/html/index.html
```

## CI

`Jenkinsfile` runs build, unit tests + JaCoCo, lint and (on `main`) a `liveUat` build on the shared
`android-jdk8` agents.
