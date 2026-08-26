# BhuSanket

BhuSanket is a landslide intelligence prototype for monitoring environmental conditions, estimating zone-level risk, and prioritising field verification across the North Eastern Region of India.

The repository currently contains two complementary implementations:

- A browser-based situation room in `frontend/` with simulated telemetry, an interactive Leaflet map, escalation scenarios, alerts, zone intelligence, and field reports.
- A standalone, deterministic Python risk engine in `backend/` with unit tests in `tests/`.

The prototype uses local and simulated inputs. It is not a live warning service and must not be used as the sole basis for evacuation, route closure, or emergency response decisions.

## Features

### Situation room

- Regional risk score and active-alert summary
- Risk, rainfall, terrain, exposure, and road map layers
- Monitored locations with selectable incident details
- Environmental telemetry for rainfall, soil moisture, temperature, and accumulated rain
- Exposure snapshot covering people, roads, villages, and critical infrastructure
- Priority queue for field verification

### Simulation and workflow

- Normal, Heavy Rain, and Extreme Rain scenarios in the dashboard
- Automatic telemetry variation during normal simulation mode
- Zone intelligence view with contributing factors and explanations
- Alerts and response-priority views
- Local field-report submission and verification log
- Data-source register showing which inputs are simulated or prepared

### Python risk engine

The Python engine calculates a bounded score from seven inputs:

| Input | Weight |
| --- | ---: |
| Current rainfall | 16% |
| Accumulated rainfall | 12% |
| Soil moisture | 20% |
| Slope | 16% |
| Terrain susceptibility | 16% |
| Historical vulnerability | 12% |
| Exposure | 8% |

Scores are classified as:

| Score | Level |
| ---: | --- |
| 0-34 | Monitoring |
| 35-54 | Advisory |
| 55-74 | High |
| 75-100 | Critical |

## Project structure

```text
.
├── backend/
│   ├── alerts.py              # Placeholder for alert-generation logic
│   ├── app.py                 # Placeholder for a future backend API
│   ├── risk_engine.py         # Deterministic risk calculation
│   ├── simulator.py           # Scenario transformations
│   └── data/                  # Reserved for future data integrations
├── frontend/
│   ├── index.html              # Dashboard shell and views
│   ├── css/style.css           # Dashboard styling and responsive layout
│   └── js/
│       ├── app.js              # State, mock data, simulation, and navigation
│       ├── dashboard.js         # Dashboard rendering and interactions
│       └── map.js               # Leaflet map and map layers
├── tests/
│   └── test_risk_engine.py     # Risk engine and simulator tests
├── requirements.txt
└── README.md
```

## Run the dashboard

The frontend is static and does not currently require a backend server or Python packages.

From the repository root:

```bash
cd frontend
python3 -m http.server 8000
```

Open [http://localhost:8000](http://localhost:8000) in a browser.

The map uses Leaflet and external tile/font resources loaded from CDNs, so those resources require network access. The dashboard still loads without the map tile services, but the map background and some map controls will be unavailable.

## Run the Python tests

The project uses Python's standard-library `unittest` framework. Run the suite from the repository root:

```bash
python3 -m unittest discover -s tests -p 'test_*.py'
```

The current tests cover validation, deterministic and bounded scores, risk thresholds, scenario escalation, and recovery behavior.

### ML comparison model

The repository also includes `backend/ml_model.py`, a deterministic Random Forest classifier trained from the clearly labeled prototype dataset in `backend/data/historical_training.json`. It compares the learned risk level with the explainable baseline, reports model confidence, and identifies the three strongest learned drivers.

This training file is synthetic prototype data, not a validated historical disaster record. Replace it with reviewed event history and evaluate it on held-out data before using model output operationally. Normal zone, alert, map, and simulation responses now include the predictive output while retaining the deterministic score for audit. The model inspection endpoint is:

```text
POST /api/ml/compare
```

It accepts the same seven numeric zone inputs as the baseline and returns `baseline`, `model`, and `comparison` objects. A disagreement is intentionally surfaced for human review rather than silently overriding the baseline.

## Use the risk engine

The engine accepts a mapping with the required numeric fields and returns a risk score, level, confidence, explanation, and contributing factors.

```python
from backend.risk_engine import calculate_risk
from backend.simulator import simulate_zone

zone = {
	"rainfall": 42.6,
	"accumulated": 184,
	"moisture": 76,
	"slope": 72,
	"susceptibility": 82,
	"history": 68,
	"exposure": 86,
}

normal = calculate_risk(zone)
extreme = simulate_zone(zone, "Extreme Rain")

print(normal["risk_score"], normal["risk_level"])
print(extreme["risk_score"], extreme["risk_level"])
```

Accepted simulator scenarios are `Normal`, `Heavy Rain`, `Extreme Rain`, and `Recovery`. Rainfall and accumulated rainfall must be non-negative. Moisture, slope, susceptibility, historical vulnerability, and exposure must be between 0 and 100.

## Current status and next steps

This is a prototype rather than a production monitoring system. The browser dashboard currently uses its own in-memory mock data, while the Python engine is a separate library and is not connected to the dashboard. Authentication and protected API foundations are present, but the repository does not yet include durable operational data persistence, live sensor ingestion, or production alert delivery.

Natural next steps are to connect the dashboard to an API backed by `risk_engine.py`, populate the data contracts in `backend/data/`, add durable report storage, and integrate validated environmental, terrain, historical, and exposure feeds.

## Configure authentication

The authentication foundation uses Firebase Authentication. It runs in demo mode while the Firebase configuration is blank, so the dashboard can still be explored locally.

### 1. Create and configure a Firebase project

Create a Firebase project, enable Email/Password, Google, and GitHub providers in **Authentication > Sign-in method**, and register a Web app. Copy its configuration into `frontend/js/config.js`. The GitHub OAuth client ID and secret are configured in Firebase, never in this repository.

```js
window.BHUSANKET_CONFIG = {
	firebase: { apiKey: '...', authDomain: '...', projectId: '...', storageBucket: '...', messagingSenderId: '...', appId: '...' },
	apiBaseUrl: 'http://localhost:8001'
};
```

The Play Games button is intentionally Android-only. Add Play Games sign-in later in the Android client and exchange its credential with Firebase.

### 2. Run the API

Install dependencies and copy the environment template:

```bash
python3 -m pip install -r requirements.txt
cp .env.example .env
```

Set `GOOGLE_APPLICATION_CREDENTIALS` in `.env` to a Firebase service-account JSON file, then start the API from the repository root:

```bash
flask --app backend.app run --host 0.0.0.0 --port 8001 --debug
```

The API accepts Firebase ID tokens as `Authorization: Bearer <token>`. Report creation and editing are restricted to `Operator`, `Field officer`, and `Admin` roles.

### 5. Assign roles

### 3. Assign roles

For security, users cannot promote themselves. Set Firebase custom claims from a trusted Admin SDK script or Admin-only server route:

```python
from firebase_admin import auth
auth.set_custom_user_claims(user_id, {'role': 'Operator'})
```

New accounts default to `Citizen`. Users must refresh their token after a role change.

## License

See [LICENSE](LICENSE).
