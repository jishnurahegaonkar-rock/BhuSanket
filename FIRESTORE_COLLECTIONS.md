# BhuSanket Firestore Collections

Firestore does not require empty collections to be created in advance. A collection appears automatically when its first document is created. The application should eventually create most of these records; the Firebase Console is useful for adding initial test data.

Create these five top-level collections:

```text
users
zones
sensors
alerts
field_reports
notification_logs
```

## How to create a collection manually

1. Open Firebase Console and select `bhusanket-861d0`.
2. Go to **Build > Firestore Database**.
3. Open the **Data** tab.
4. Click **Start collection** if there are no collections, or click **+ Start collection**.
5. Enter the exact **Collection ID** from this document.
6. Click **Next**.
7. Enter the **Document ID**. Use an automatic ID unless this document describes a fixed ID.
8. Add each field using the exact field name and type listed below.
9. Click **Save**.

The three concepts in the Firebase form are:

- **Collection ID**: the parent group, such as `zones`.
- **Document ID**: the unique record inside that collection, such as `tawang`.
- **Fields**: the properties stored in that document, such as `name` and `riskScore`.

Do not use spaces or capital letters in collection IDs or document IDs.

## 1. `users`

One document per Firebase Authentication user. Use the user's Firebase UID as the document ID.

Example:

```text
Collection ID: users
Document ID: 8XyExampleFirebaseUid
```

| Field name | Firestore type | Example | Required |
|---|---|---|---|
| `email` | string | `operator@example.com` | yes |
| `displayName` | string | `Field Operator` | yes |
| `role` | string | `Citizen` | yes |
| `createdAt` | timestamp | current date/time | yes |
| `active` | boolean | `true` | yes |

Allowed role strings:

```text
Citizen
Operator
Field officer
District official
Admin
```

The `role` field is for profile data. Real authorization uses the Firebase custom claim set by the Admin SDK. Users must never be allowed to change their own role.

## 2. `zones`

One document per monitored location. Use a stable lowercase ID matching the application's zone ID.

Example:

```text
Collection ID: zones
Document ID: tawang
```

| Field name | Firestore type | Example |
|---|---|---|
| `name` | string | `Tawang Corridor` |
| `district` | string | `Tawang, Arunachal Pradesh` |
| `riskScore` | number | `67` |
| `riskLevel` | string | `High` |
| `latitude` | number | `27.4728` |
| `longitude` | number | `94.912` |
| `active` | boolean | `true` |
| `updatedAt` | timestamp | current date/time |

Suggested document IDs:

```text
tawang
siang
chura
garo
bomdila
ziro
roing
ukhrul
```

## 3. `sensors`

One document per sensor device. Use a stable device ID as the document ID.

Example:

```text
Collection ID: sensors
Document ID: sensor-tawang-001
```

| Field name | Firestore type | Example |
|---|---|---|
| `zoneId` | string | `tawang` |
| `rainfall` | number | `42.6` |
| `soilMoisture` | number | `76` |
| `temperature` | number | `19.4` |
| `accumulatedRainfall` | number | `184` |
| `status` | string | `online` |
| `updatedAt` | timestamp | current date/time |

Use numbers for all measurements. Do not enter `42.6 mm`, `76%`, or other units inside a number field.

## 4. `alerts`

One document per generated alert. Use an automatic ID for alerts created by the application.

Example:

```text
Collection ID: alerts
Document ID: Auto-ID
```

| Field name | Firestore type | Example |
|---|---|---|
| `zoneId` | string | `tawang` |
| `title` | string | `Heavy rainfall detected` |
| `level` | string | `High` |
| `riskScore` | number | `67` |
| `reason` | string | `Rising rainfall and soil saturation` |
| `active` | boolean | `true` |
| `createdAt` | timestamp | current date/time |
| `createdBy` | string | `system` |

Allowed alert levels:

```text
Monitoring
Advisory
High
Critical
```

## 5. `field_reports`

One document per field observation. Operators and Field officers can create these reports. Admins can manage them.

Example:

```text
Collection ID: field_reports
Document ID: Auto-ID
```

| Field name | Firestore type | Example |
|---|---|---|
| `zoneId` | string | `tawang` |
| `location` | string | `Tawang Corridor` |
| `observation` | string | `Fresh tension cracks observed` |
| `severity` | string | `High` |
| `status` | string | `Under review` |
| `submittedBy` | string | Firebase user UID |
| `createdAt` | timestamp | current date/time |
| `latitude` | number | `27.4728` |
| `longitude` | number | `94.912` |
| `accuracyM` | number | `12.5` |
| `mediaUrl` | string | optional Firebase Storage URL |

Allowed report status strings:

```text
Submitted
Under review
Verified
Rejected
```

Allowed severity strings:

```text
Advisory
High
Critical
```

The `submittedBy` value must be the Firebase Authentication UID of the signed-in operator. It must not be an email address or display name. This is required by the security rule:

```text
request.resource.data.submittedBy == request.auth.uid
```

## 6. `notification_logs`

One document per simulated or real delivery attempt. This collection is planned for the Firestore migration; the current prototype stores these records in SQLite.

Example:

```text
Collection ID: notification_logs
Document ID: Auto-ID
```

| Field name | Firestore type | Example |
|---|---|---|
| `alertId` | string | `alert-tawang-001` |
| `zoneId` | string | `tawang` |
| `channel` | string | `sms` |
| `language` | string | `hi` |
| `templateKey` | string | `landslide_high_hi` |
| `message` | string | Localized alert message |
| `status` | string | `Simulated` |
| `provider` | string | `prototype` |
| `createdAt` | timestamp | current date/time |

Planned channels are `app`, `sms`, `email`, and `ivr`. Current simulation creates `app` and `sms` records only. Planned statuses include `Queued`, `Sent`, `Delivered`, `Failed`, and `Simulated`.

Supported template language codes are `en` (English), `hi` (Hindi), `as` (Assamese), `mni` (Manipuri), and `kha` (Khasi). Templates are defined in `backend/notifications.py` and should be reviewed by native-language operators before operational use.

## Recommended first test data

Create one document in each collection in this order:

1. `users` with the UID of an existing Firebase user.
2. `zones/tawang`.
3. `sensors/sensor-tawang-001`.
4. `alerts/alert-tawang-001`.
5. `field_reports/report-demo-001`.

For the first `users` document, set `role` to `Citizen`. Assign `Operator` later through Firebase custom claims from the Admin SDK. Do not test by manually changing only the Firestore `role` field.

## Rules deployment

The repository's [firestore.rules](firestore.rules) file contains the RBAC rules. Copy its contents into **Firestore Database > Rules**, then click **Publish**.

The current Flask dashboard still reads its operational data from the local SQLite database. These collections are the Firestore structure for the migration; creating them manually will not yet replace the local dashboard data until the backend data layer is switched to Firestore.
