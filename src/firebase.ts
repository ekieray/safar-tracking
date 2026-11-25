import { initializeApp } from 'firebase/app';
import {
  createUserWithEmailAndPassword,
  FacebookAuthProvider,
  getAuth,
  getRedirectResult,
  GoogleAuthProvider,
  onAuthStateChanged,
  sendPasswordResetEmail,
  signInAnonymously,
  signInWithEmailAndPassword,
  signInWithPopup,
  signInWithRedirect,
  signOut,
  updateProfile,
  User,
} from 'firebase/auth';
import {
  doc,
  getDocFromServer,
  getFirestore,
} from 'firebase/firestore';
import firebaseConfig from '../firebase-applet-config.json';

const app = initializeApp(firebaseConfig);

export const db = firebaseConfig.firestoreDatabaseId
  ? getFirestore(app, firebaseConfig.firestoreDatabaseId)
  : getFirestore(app);

export const auth = getAuth(app);

const googleProvider = new GoogleAuthProvider();
googleProvider.setCustomParameters({ prompt: 'select_account' });

const facebookProvider = new FacebookAuthProvider();
facebookProvider.addScope('email');
facebookProvider.addScope('public_profile');

export enum OperationType {
  CREATE = 'create',
  UPDATE = 'update',
  DELETE = 'delete',
  LIST = 'list',
  GET = 'get',
  WRITE = 'write',
}

export interface FirestoreErrorInfo {
  error: string;
  operationType: OperationType;
  path: string | null;
  authInfo: {
    userId?: string | null;
    email?: string | null;
    emailVerified?: boolean | null;
    isAnonymous?: boolean | null;
    tenantId?: string | null;
    providerInfo?: {
      providerId?: string | null;
      email?: string | null;
    }[];
  };
}

export function handleFirestoreError(
  error: unknown,
  operationType: OperationType,
  path: string | null
): never {
  const errInfo: FirestoreErrorInfo = {
    error: error instanceof Error ? error.message : String(error),
    authInfo: {
      userId: auth.currentUser?.uid,
      email: auth.currentUser?.email,
      emailVerified: auth.currentUser?.emailVerified,
      isAnonymous: auth.currentUser?.isAnonymous,
      tenantId: auth.currentUser?.tenantId,
      providerInfo:
        auth.currentUser?.providerData?.map((provider) => ({
          providerId: provider.providerId,
          email: provider.email,
        })) || [],
    },
    operationType,
    path,
  };
  console.error('Firestore Error: ', JSON.stringify(errInfo));
  throw new Error(JSON.stringify(errInfo));
}

export async function testFirestoreConnection(): Promise<boolean> {
  try {
    await getDocFromServer(doc(db, 'test', 'connection'));
    return true;
  } catch (error) {
    if (
      error instanceof Error &&
      error.message.includes('the client is offline')
    ) {
      console.error('Please check your Firebase configuration.');
      return false;
    }
    return true;
  }
}

const REDIRECT_MODE_STORAGE_KEY = 'konvoiku_pending_google_redirect';

function isRunningInIframe(): boolean {
  try {
    return typeof window !== 'undefined' && window.self !== window.top;
  } catch {
    return true;
  }
}

function shouldFallbackToRedirect(error: unknown): boolean {
  const code =
    typeof error === 'object' && error !== null && 'code' in error
      ? String((error as { code?: unknown }).code || '')
      : '';
  return (
    code === 'auth/popup-blocked' ||
    code === 'auth/operation-not-supported-in-this-environment'
  );
}

export async function signInWithGoogle(): Promise<{
  user: User | null;
  redirected: boolean;
}> {
  try {
    const cred = await signInWithPopup(auth, googleProvider);
    return { user: cred.user, redirected: false };
  } catch (error) {
    if (shouldFallbackToRedirect(error) && !isRunningInIframe()) {
      try {
        sessionStorage.setItem(REDIRECT_MODE_STORAGE_KEY, 'web');
      } catch {
        // ignore storage restrictions
      }
      await signInWithRedirect(auth, googleProvider);
      return { user: null, redirected: true };
    }
    throw error;
  }
}

export async function signInWithGoogleForMobileBridge(): Promise<{
  user: User | null;
  idToken: string | null;
  redirected: boolean;
}> {
  try {
    const cred = await signInWithPopup(auth, googleProvider);
    const oauthCredential = GoogleAuthProvider.credentialFromResult(cred);
    return {
      user: cred.user,
      idToken: oauthCredential?.idToken ?? null,
      redirected: false,
    };
  } catch (error) {
    if (shouldFallbackToRedirect(error) && !isRunningInIframe()) {
      try {
        sessionStorage.setItem(REDIRECT_MODE_STORAGE_KEY, 'mobile_bridge');
      } catch {
        // ignore storage restrictions
      }
      await signInWithRedirect(auth, googleProvider);
      return { user: null, idToken: null, redirected: true };
    }
    throw error;
  }
}

export async function consumeGoogleRedirectResult(): Promise<{
  mode: 'web' | 'mobile_bridge' | null;
  user: User | null;
  idToken: string | null;
}> {
  let pendingMode: 'web' | 'mobile_bridge' | null = null;
  try {
    const stored = sessionStorage.getItem(REDIRECT_MODE_STORAGE_KEY);
    if (stored === 'mobile_bridge' || stored === 'web') {
      pendingMode = stored;
    }
  } catch {
    pendingMode = null;
  }

  try {
    const result = await getRedirectResult(auth);
    try {
      sessionStorage.removeItem(REDIRECT_MODE_STORAGE_KEY);
    } catch {
      // ignore
    }
    if (!result) {
      return { mode: null, user: null, idToken: null };
    }
    const oauthCredential = GoogleAuthProvider.credentialFromResult(result);
    return {
      mode: pendingMode ?? 'web',
      user: result.user,
      idToken: oauthCredential?.idToken ?? null,
    };
  } catch (err) {
    try {
      sessionStorage.removeItem(REDIRECT_MODE_STORAGE_KEY);
    } catch {
      // ignore
    }
    throw err;
  }
}

export async function signInWithFacebook(): Promise<User> {
  const cred = await signInWithPopup(auth, facebookProvider);
  return cred.user;
}

export async function signUpWithEmail(
  fullName: string,
  email: string,
  password: string
): Promise<User> {
  const cred = await createUserWithEmailAndPassword(auth, email.trim(), password);
  const cleanName = fullName.trim();
  if (cleanName) {
    await updateProfile(cred.user, { displayName: cleanName });
  }
  return cred.user;
}

export async function signInWithEmail(
  email: string,
  password: string
): Promise<User> {
  const cred = await signInWithEmailAndPassword(auth, email.trim(), password);
  return cred.user;
}

export async function sendResetPasswordEmail(email: string): Promise<void> {
  await sendPasswordResetEmail(auth, email.trim());
}

export async function signOutFirebase(): Promise<void> {
  await signOut(auth);
}

const CLOUD_DEVICE_AUTH_KEY = 'konvoiku_cloud_device_auth_v1';

export function isDeviceCloudGuestEmail(email?: string | null): boolean {
  return (email || '').trim().toLowerCase().endsWith('@guest.konvoiku.id');
}

function generateRandomTokenPart(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID().replace(/-/g, '');
  }
  return `${Date.now().toString(36)}${Math.random().toString(36).slice(2, 12)}`;
}

export async function ensureCloudSignedInUser(): Promise<User> {
  if (auth.currentUser) {
    return auth.currentUser;
  }

  if (typeof window !== 'undefined') {
    try {
      const raw = window.localStorage.getItem(CLOUD_DEVICE_AUTH_KEY);
      if (raw) {
        const parsed = JSON.parse(raw) as { email?: string; password?: string };
        if (parsed.email && parsed.password) {
          const cred = await signInWithEmailAndPassword(
            auth,
            parsed.email,
            parsed.password
          );
          return cred.user;
        }
      }
    } catch {
      // Fall through to anonymous or fresh device provisioning
    }
  }

  try {
    const anonCred = await signInAnonymously(auth);
    return anonCred.user;
  } catch {
    // Fallback when Anonymous Auth is disabled (ADMIN_ONLY_OPERATION):
    const deviceEmail = `rider.${generateRandomTokenPart().slice(0, 16)}@guest.konvoiku.id`;
    const devicePassword = `Knv#${generateRandomTokenPart().slice(0, 24)}`;
    const cred = await createUserWithEmailAndPassword(
      auth,
      deviceEmail,
      devicePassword
    );
    if (typeof window !== 'undefined') {
      try {
        window.localStorage.setItem(
          CLOUD_DEVICE_AUTH_KEY,
          JSON.stringify({ email: deviceEmail, password: devicePassword })
        );
      } catch {
        // Ignore storage error
      }
    }
    return cred.user;
  }
}

export function subscribeToAuthState(
  callback: (user: User | null) => void
): () => void {
  return onAuthStateChanged(auth, callback);
}

export function mapFirebaseAuthError(error: unknown, providerLabel = 'Akun'): string {
  const code =
    typeof error === 'object' && error !== null && 'code' in error
      ? String((error as { code?: unknown }).code || '')
      : '';
  const msg = error instanceof Error ? error.message : String(error || '');
  const upperMsg = msg.toUpperCase();

  if (code === 'auth/unauthorized-domain') {
    return `Domain browser saat ini (${typeof window !== 'undefined' ? window.location.hostname : 'host'}) belum ditambahkan ke Firebase Console → Authentication → Settings → Authorized domains.`;
  }
  if (code === 'auth/popup-blocked') {
    return 'Jendela popup Google diblokir oleh browser. Izinkan pop-up untuk situs ini atau buka di tab browser utama.';
  }
  if (
    code === 'auth/operation-not-allowed' ||
    code === 'auth/admin-restricted-operation' ||
    upperMsg.includes('OPERATION_NOT_ALLOWED') ||
    upperMsg.includes('ADMIN_ONLY_OPERATION') ||
    upperMsg.includes('PASSWORD_LOGIN_DISABLED')
  ) {
    return `Metode ${providerLabel} / Tamu Cloud belum diaktifkan di Firebase Console (Authentication → Sign-in method). Saat ini hanya Google Sign-In yang aktif, atau aktifkan Email/Password & Anonymous di Firebase Console.`;
  }
  if (code === 'auth/popup-closed-by-user' || code === 'auth/cancelled-popup-request') {
    return `Jendela login ${providerLabel} ditutup sebelum selesai. Silakan coba lagi.`;
  }
  if (code === 'auth/account-exists-with-different-credential') {
    return 'Email ini sudah terdaftar menggunakan metode login lain (mis. Google atau Email). Masuk dengan metode tersebut lalu sinkronkan profil.';
  }
  if (code === 'auth/email-already-in-use') {
    return 'Email sudah terdaftar. Silakan pindah ke tab Masuk (Sign In).';
  }
  if (code === 'auth/invalid-credential' || code === 'auth/wrong-password' || code === 'auth/user-not-found') {
    return 'Email atau kata sandi salah. Periksa kembali atau klik Daftar untuk membuat akun baru.';
  }
  if (code === 'auth/weak-password') {
    return 'Kata sandi terlalu lemah. Gunakan minimal 6 karakter.';
  }
  if (code === 'auth/invalid-email') {
    return 'Format alamat email tidak valid.';
  }
  if (code === 'auth/too-many-requests') {
    return 'Terlalu banyak percobaan login. Tunggu beberapa saat lalu coba lagi.';
  }
  if (msg.includes('popup')) {
    return `Popup login ${providerLabel} diblokir atau ditutup. Izinkan popup browser lalu coba lagi.`;
  }
  return `Gagal autentikasi ${providerLabel}. Periksa koneksi internet & konfigurasi Firebase.`;
}
