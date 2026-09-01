const BhuSanketAuth = (() => {
    let firebaseAuth;
    let currentUser;
    let role = 'Citizen';
    let mode = 'signin';
    const config = window.BHUSANKET_CONFIG || {};
    const firebaseConfig = config.firebase || {};
    const localDemo = window.location.protocol === 'file:'
        || window.location.hostname === 'localhost'
        || window.location.hostname === '127.0.0.1';
    const configured = !localDemo && Boolean(firebaseConfig.apiKey && firebaseConfig.projectId && window.firebase);
    const roles = ['Admin', 'District official', 'Operator', 'Field officer', 'Citizen'];
    const privilegedRoles = ['Admin', 'Operator', 'Field officer'];
    const $ = id => document.getElementById(id);

    function message(text, error = false) {
        const element = $('auth-message');
        element.textContent = text;
        element.classList.toggle('error', error);
    }

    function setBusy(busy) {
        $('submit-auth').disabled = busy;
        $('switch-auth').disabled = busy;
    }

    function setVisible(visible) {
        const shell = $('auth-shell');
        shell.classList.toggle('hidden', !visible);
        shell.hidden = !visible;
        document.body.classList.toggle('auth-locked', visible);
    }

    function renderShell() {
        document.body.classList.add('auth-locked');
        const shell = document.createElement('div');
        shell.id = 'auth-shell';
        shell.innerHTML = `
            <div class="auth-card" role="dialog" aria-modal="true" aria-labelledby="auth-title">
                <aside class="auth-story">
                    <div class="auth-story-brand">
                        <span class="auth-mark">&#9650;</span>
                        <span class="auth-story-brand-name">BHUSANKET</span>
                    </div>
                    <p class="auth-story-kicker">Landslide Intelligence Network</p>
                    <div class="auth-signal"></div>
                    <div class="auth-story-copy">
                        <p class="kicker">REGIONAL WATCH</p>
                        <h2>A clearer view of<br><em>changing ground</em> conditions.</h2>
                        <p>Real-time terrain risk, environmental telemetry, and field coordination in one workspace.</p>
                    </div>
                    <div class="auth-story-foot">
                        <span><i class="pulse-dot"></i>24 / 7 monitoring</span>
                        <span>8 priority zones</span>
                    </div>
                </aside>
                <section class="auth-panel">
                    <div class="auth-panel-top">
                        <div class="auth-panel-brand">
                            <span class="auth-panel-brand-name">BHUSANKET</span>
                            <span class="auth-panel-brand-sub">Secure Access</span>
                        </div>
                        <span class="auth-live"><i class="pulse-dot"></i>System online</span>
                    </div>

                    <div class="auth-heading-row">
                        <p class="auth-kicker">SECURE ACCESS / REGIONAL WATCH</p>
                        <span class="auth-step" id="auth-mode-label">SIGN IN</span>
                    </div>
                    <h1 class="auth-title" id="auth-title">Sign in to the situation room</h1>
                    <p class="auth-copy" id="auth-copy">Access alerts, zone intelligence, and field coordination tools.</p>

                    <div class="auth-provider-grid">
                        <button type="button" class="auth-provider-btn" id="google-login">
                            <span class="auth-provider-icon google-icon">G</span>
                            <span>Continue with Google</span>
                        </button>
                        <button type="button" class="auth-provider-btn" id="github-login">
                            <span class="auth-provider-icon">&#9670;</span>
                            <span>Continue with GitHub</span>
                        </button>
                    </div>

                    <div class="auth-divider"><span>or use email and password</span></div>

                    <form id="email-login" class="auth-form" novalidate>
                        <label class="auth-field" id="name-field" hidden>
                            <span class="auth-field-label">Full name</span>
                            <input class="auth-input" id="auth-name" type="text" autocomplete="name" placeholder="Your name">
                        </label>

                        <label class="auth-field">
                            <span class="auth-field-label">Email address</span>
                            <input class="auth-input" id="auth-email" type="email" autocomplete="email" required placeholder="you@example.com">
                        </label>

                        <label class="auth-field">
                            <span class="auth-field-label">Password</span>
                            <div class="password-field">
                                <input class="auth-input" id="auth-password" type="password" autocomplete="current-password" minlength="8" required placeholder="At least 8 characters">
                                <button type="button" class="password-toggle" id="toggle-password">Show</button>
                            </div>
                        </label>

                        <label class="auth-field" id="confirm-field" hidden>
                            <span class="auth-field-label">Confirm password</span>
                            <div class="password-field">
                                <input class="auth-input" id="auth-confirm" type="password" autocomplete="new-password" minlength="8" placeholder="Repeat password">
                                <button type="button" class="password-toggle" id="toggle-confirm">Show</button>
                            </div>
                        </label>

                        <div class="auth-under-row">
                            <button type="button" class="auth-forgot" id="forgot-login">Forgot password?</button>
                        </div>

                        <button type="submit" class="auth-submit" id="submit-auth">
                            <span id="submit-auth-label">Sign in</span>
                            <span class="arrow">&#8594;</span>
                        </button>
                    </form>

                    <p class="auth-message" id="auth-message" role="status" aria-live="polite"></p>

                    <div class="auth-switch">
                        <span id="switch-copy">New to BhuSanket?</span>
                        <button type="button" class="auth-link" id="switch-auth">
                            <span id="switch-auth-label">Create an account</span>
                            <span>&#8594;</span>
                        </button>
                    </div>

                    <small class="auth-demo-note" id="demo-note"></small>
                </section>
            </div>`;
        document.body.prepend(shell);

        $('email-login').addEventListener('submit', signInWithEmail);
        $('google-login').addEventListener('click', () => signInWithProvider('google'));
        $('github-login').addEventListener('click', () => signInWithProvider('github'));
        $('forgot-login').addEventListener('click', resetPassword);
        $('switch-auth').addEventListener('click', toggleMode);
        $('toggle-password').addEventListener('click', () => togglePassword('auth-password', 'toggle-password'));
        $('toggle-confirm').addEventListener('click', () => togglePassword('auth-confirm', 'toggle-confirm'));
        updateMode();
    }

    function updateMode() {
        const signup = mode === 'signup';
        $('auth-mode-label').textContent = signup ? 'SIGN UP' : 'SIGN IN';
        $('auth-title').textContent = signup ? 'Create your field account' : 'Sign in to the situation room';
        $('auth-copy').textContent = signup
            ? 'Create a secure account to access your assigned BhuSanket workspace.'
            : 'Access alerts, zone intelligence, and field coordination tools.';
        $('submit-auth-label').textContent = signup ? 'Create account' : 'Sign in';
        $('switch-copy').textContent = signup ? 'Already have an account?' : 'New to BhuSanket?';
        $('switch-auth-label').textContent = signup ? 'Sign in' : 'Create an account';
        $('name-field').hidden = !signup;
        $('confirm-field').hidden = !signup;
        $('forgot-login').hidden = signup;
        $('auth-name').required = signup;
        $('auth-confirm').required = signup;
        $('auth-password').autocomplete = signup ? 'new-password' : 'current-password';
    }

    function toggleMode() {
        mode = mode === 'signin' ? 'signup' : 'signin';
        $('email-login').reset();
        message('');
        updateMode();
    }

    function addUserBadge(label, user = null) {
        const deck = document.querySelector('.deck-status');
        if (!deck || document.getElementById('auth-user-badge')) return;
        const badge = document.createElement('span');
        badge.id = 'auth-user-badge';
        badge.className = 'auth-user-badge';
        badge.innerHTML = `<button id="profile-toggle" class="profile-toggle" aria-expanded="false" aria-controls="profile-menu"><i class="dot cyan"></i><span class="profile-label"></span><b class="chevron">&#8964;</b></button><div id="profile-menu" class="profile-menu" role="dialog" aria-label="User settings"><div class="profile-menu-heading"><span class="profile-menu-icon">&#9881;</span><div><strong>User settings</strong><small>Account and access</small></div></div><div class="profile-account"><span class="profile-caption">SIGNED IN AS</span><strong class="profile-name"></strong><small class="profile-email"></small></div><div class="profile-role-row"><span>ROLE</span><b class="profile-role"></b></div><div class="profile-future"><span class="profile-caption">COMING LATER</span><div class="profile-future-row"><span>SMS alerts</span><em>PLANNED</em></div><div class="profile-future-row"><span>Notification sound</span><em>PLANNED</em></div></div><button id="auth-logout" class="profile-logout">Sign out <span>&#8618;</span></button></div>`;
        badge.querySelector('.profile-label').textContent = label;
        badge.querySelector('.profile-name').textContent = user?.displayName || label;
        badge.querySelector('.profile-email').textContent = user?.email || '';
        badge.querySelector('.profile-role').textContent = `Role: ${role}`;
        deck.prepend(badge);
        $('profile-toggle').addEventListener('click', () => {
            const open = badge.classList.toggle('open');
            $('profile-toggle').setAttribute('aria-expanded', String(open));
        });
        $('auth-logout').addEventListener('click', signOut);
        document.addEventListener('click', event => {
            if (!badge.contains(event.target)) {
                badge.classList.remove('open');
                $('profile-toggle')?.setAttribute('aria-expanded', 'false');
            }
        });
    }

    function updateAccess(nextRole) {
        role = roles.includes(nextRole) ? nextRole : 'Citizen';
        document.querySelectorAll('[data-rbac]').forEach(element => {
            const allowed = element.dataset.rbac.split(',').map(item => item.trim());
            element.classList.toggle('rbac-hidden', !allowed.includes(role));
        });
        const canAccessReports = privilegedRoles.includes(role);
        document.querySelector('.nav-item[data-view="reports"]')?.classList.toggle('rbac-hidden', !canAccessReports);
        document.querySelector('[data-section="reports"]')?.classList.toggle('rbac-hidden', !canAccessReports);
        const reportForm = document.getElementById('report-form')?.closest('.report-form');
        reportForm?.classList.toggle('rbac-hidden', !canAccessReports);
        document.dispatchEvent(new CustomEvent('bhusanket:role-changed', { detail: { role } }));
    }

    async function signInWithProvider(providerName) {
        if (!firebaseAuth) return message('Firebase authentication is not configured.', true);
        const provider = providerName === 'github' ? new firebase.auth.GithubAuthProvider() : new firebase.auth.GoogleAuthProvider();
        setBusy(true);
        try {
            await firebaseAuth.signInWithPopup(provider);
        } catch (error) {
            message(error.message, true);
        } finally {
            setBusy(false);
        }
    }

    async function signInWithEmail(event) {
        event.preventDefault();
        if (!firebaseAuth) return message('Firebase authentication is not configured.', true);
        const email = $('auth-email').value.trim();
        const password = $('auth-password').value;
        if (!email || !password) return message('Enter your email and password.', true);
        setBusy(true);
        try {
            if (mode === 'signup') {
                if (password !== $('auth-confirm').value) {
                    setBusy(false);
                    return message('Passwords do not match.', true);
                }
                await firebaseAuth.createUserWithEmailAndPassword(email, password);
                await firebaseAuth.currentUser.updateProfile({ displayName: $('auth-name').value.trim() });
                message('Account created. Opening your workspace...');
            } else {
                await firebaseAuth.signInWithEmailAndPassword(email, password);
            }
        } catch (error) {
            message(error.message, true);
        } finally {
            setBusy(false);
        }
    }

    async function resetPassword() {
        if (!firebaseAuth) return message('Firebase authentication is not configured.', true);
        const email = $('auth-email').value.trim();
        if (!email) return message('Enter your email address first.', true);
        try {
            await firebaseAuth.sendPasswordResetEmail(email);
            message('Password reset instructions sent to your email.');
        } catch (error) {
            message(error.message, true);
        }
    }

    function togglePassword(inputId, buttonId) {
        const input = $(inputId);
        const visible = input.type === 'text';
        input.type = visible ? 'password' : 'text';
        $(buttonId).textContent = visible ? 'Show' : 'Hide';
    }

    async function signOut() {
        if (firebaseAuth) await firebaseAuth.signOut();
        currentUser = null;
        document.getElementById('auth-user-badge')?.remove();
        updateAccess('Citizen');
        setVisible(true);
    }

    function applyUser(user) {
        currentUser = user;
        if (!user) return setVisible(true);
        user.getIdTokenResult(true).then(token => {
            updateAccess(token.claims.role || 'Citizen');
            addUserBadge(role, user);
            setVisible(false);
            document.dispatchEvent(new CustomEvent('bhusanket:authenticated', { detail: { user, role } }));
        });
    }

    function init() {
        renderShell();
        if (!configured) {
            $('demo-note').textContent = 'Demo mode is active. Add Firebase config to enable secure authentication.';
            setVisible(false);
            updateAccess('Citizen');
            addUserBadge('Demo mode');
            return;
        }
        firebase.initializeApp(firebaseConfig);
        firebaseAuth = firebase.auth();
        firebaseAuth.setPersistence(firebase.auth.Auth.Persistence.LOCAL);
        firebaseAuth.onAuthStateChanged(applyUser);
    }

    return { init, getUser: () => currentUser, getRole: () => role, hasRole: (...allowed) => allowed.includes(role), isConfigured: () => configured, roles, privilegedRoles };
})();

window.BhuSanketAuth = BhuSanketAuth;
document.addEventListener('DOMContentLoaded', () => BhuSanketAuth.init());