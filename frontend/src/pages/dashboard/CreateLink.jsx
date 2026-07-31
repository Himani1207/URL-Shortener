import { useEffect, useMemo, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import PageHeader from '../../components/layout/PageHeader';
import Card, { CardHeader } from '../../components/ui/Card';
import Button from '../../components/ui/Button';
import Input from '../../components/ui/Input';
import PasswordInput from '../../components/ui/PasswordInput';
import Toggle from '../../components/ui/Toggle';
import QrPreview from '../../components/create/QrPreview';
import LinkSuccessCard from '../../components/create/LinkSuccessCard';
import { IconLock } from '../../components/ui/Icons';
import { ApiError, linksApi, shortLinkOrigin } from '../../lib/api';
import { useToast } from '../../context/ToastContext';
import { DEFAULT_QR_BACKGROUND, DEFAULT_QR_FOREGROUND } from '../../lib/qr';

/**
 * Everything needed to create a link, on one page.
 *
 * <b>Layout.</b> Two columns from `xl`: configuration on the left, the QR preview on
 * the right. Below that the preview moves underneath the form, which is the right
 * order on a narrow screen — the QR is a consequence of the destination URL, so
 * reading order should follow cause to effect. The breakpoint is `xl` rather than
 * `lg` because the dashboard already spends 256px on its sidebar; splitting a
 * 1024px viewport into two columns on top of that leaves both too narrow.
 *
 * <b>Validation.</b> Runs client-side first so obvious mistakes never cost a round
 * trip, and the server's `fieldErrors` map is merged into the same state on failure.
 * One error display, two sources — the alternative is a form where a client error
 * and a server error look different for no reason the user can perceive.
 *
 * <b>State.</b> `created` is what switches the page into its success state. Keeping
 * the form state alongside it is what makes "Create another" able to clear the
 * fields while keeping the QR colours the user picked.
 */

/** Mirrors the backend's alias rules so the message arrives before the request. */
const ALIAS_PATTERN = /^[A-Za-z0-9_-]{3,50}$/;

const MIN_PASSWORD_LENGTH = 4;

export default function CreateLink() {
  const toast = useToast();
  const location = useLocation();
  const navigate = useNavigate();

  /**
   * Seeded from the landing page's hero input, which routes here rather than
   * creating anything itself. Read during the first render so the field is never
   * briefly empty, and cleared from history immediately below.
   */
  const [originalUrl, setOriginalUrl] = useState(() => location.state?.pendingUrl ?? '');
  const [customAlias, setCustomAlias] = useState('');
  const [expiresAt, setExpiresAt] = useState('');
  const [passwordEnabled, setPasswordEnabled] = useState(false);
  const [password, setPassword] = useState('');

  const [foreground, setForeground] = useState(DEFAULT_QR_FOREGROUND);
  const [background, setBackground] = useState(DEFAULT_QR_BACKGROUND);
  const [qrSize, setQrSize] = useState('medium');

  const [submitting, setSubmitting] = useState(false);
  const [fieldErrors, setFieldErrors] = useState({});
  const [created, setCreated] = useState(null);

  /**
   * Drops the seeded URL out of history once it has been read.
   *
   * Without this, navigating away and pressing back would refill a field the user
   * may have deliberately cleared, and a refresh would resurrect a URL from a
   * session that ended long ago.
   */
  useEffect(() => {
    if (location.state?.pendingUrl) {
      navigate(location.pathname, { replace: true, state: null });
    }
  }, [location.state, location.pathname, navigate]);

  /**
   * What the preview encodes.
   *
   * With an alias, the short link is already known — it is the origin plus the
   * alias — so the preview shows the real thing. Without one, the code is assigned
   * by the server at creation time and cannot be known yet, so the preview falls
   * back to the destination. The caption below the QR says which of the two is on
   * screen, rather than letting the user assume.
   */
  const previewValue = useMemo(() => {
    const alias = customAlias.trim();
    if (alias && ALIAS_PATTERN.test(alias)) return `${shortLinkOrigin()}/${alias}`;

    const url = originalUrl.trim();
    return url ? normaliseUrl(url) : '';
  }, [customAlias, originalUrl]);

  const previewCaption = useMemo(() => {
    if (!previewValue) return null;

    const alias = customAlias.trim();
    if (alias && ALIAS_PATTERN.test(alias)) return 'Encoding your short link';
    return 'Encoding the destination — regenerates for the short link once created';
  }, [previewValue, customAlias]);

  /** Client-side checks. Returns a field/message map; empty means valid. */
  const validate = () => {
    const errors = {};

    const url = originalUrl.trim();
    if (!url) {
      errors.originalUrl = 'A destination URL is required';
    } else if (!isProbablyUrl(normaliseUrl(url))) {
      errors.originalUrl = 'That does not look like a valid URL';
    }

    const alias = customAlias.trim();
    if (alias && !ALIAS_PATTERN.test(alias)) {
      errors.customAlias =
        'Use 3–50 letters, numbers, hyphens or underscores';
    }

    if (expiresAt && new Date(expiresAt).getTime() <= Date.now()) {
      errors.expiresAt = 'Pick a date in the future';
    }

    if (passwordEnabled) {
      if (!password) {
        errors.password = 'Enter a password, or turn protection off';
      } else if (password.length < MIN_PASSWORD_LENGTH) {
        errors.password = `Use at least ${MIN_PASSWORD_LENGTH} characters`;
      }
    }

    return errors;
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    const errors = validate();
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) return;

    const alias = customAlias.trim();

    setSubmitting(true);
    try {
      const link = await linksApi.create({
        originalUrl: normaliseUrl(originalUrl.trim()),
        customAlias: alias || undefined,
        // <input type="datetime-local"> yields "2026-12-31T23:59", which the
        // backend's LocalDateTime parses directly.
        expiresAt: expiresAt || undefined,
        // Omitted entirely when the toggle is off, so the server never has to
        // interpret an empty string as "no password".
        password: passwordEnabled ? password : undefined,
      });

      // The response has no customAlias field — the server returns the short code
      // it assigned, whether generated or claimed. Carrying the alias through here
      // is what lets the success card distinguish the two.
      setCreated({ ...link, customAlias: alias || null });

      // Clear the secret from component state the moment it is no longer needed.
      setPassword('');
      toast.success('Link created');
    } catch (error) {
      applyServerError(error, setFieldErrors, toast);
    } finally {
      setSubmitting(false);
    }
  };

  /** Resets the form but keeps the QR styling — that is a preference, not input. */
  const handleCreateAnother = () => {
    setCreated(null);
    setOriginalUrl('');
    setCustomAlias('');
    setExpiresAt('');
    setPasswordEnabled(false);
    setPassword('');
    setFieldErrors({});
  };

  if (created) {
    return (
      <>
        <PageHeader
          title="Create link"
          description="Your link is ready to share."
        />
        <LinkSuccessCard
          link={created}
          qr={{ foreground, background, size: qrSize }}
          onCreateAnother={handleCreateAnother}
        />
      </>
    );
  }

  return (
    <>
      <PageHeader
        title="Create link"
        description="Set the destination, tune the options you need, and generate a short link with its QR code."
      />

      <form onSubmit={handleSubmit} noValidate>
        {/* Two columns from lg rather than xl. With the sidebar gone there is 256px
            more to work with, so the split no longer needs a 1280px viewport. */}
        <div className="grid items-start gap-6 lg:grid-cols-[minmax(0,1fr)_21rem] lg:gap-8">
          {/* ---------------- Configuration ---------------- */}
          <div className="space-y-6">
            <Card>
              <CardHeader
                title="Destination"
                description="Where people end up after following your short link."
              />

              <div className="mt-6 space-y-5">
                <Input
                  label="Destination URL"
                  type="text"
                  inputMode="url"
                  autoComplete="off"
                  spellCheck="false"
                  placeholder="example.com/a/very/long/path"
                  value={originalUrl}
                  onChange={(event) => setOriginalUrl(event.target.value)}
                  error={fieldErrors.originalUrl}
                  hint="https:// is added for you if you leave it off"
                  required
                />

                {/* The resulting address is shown as a hint rather than as an
                    inline prefix: the origin can be a long hostname, and baking it
                    into the field would leave a phone-width input with almost no
                    room left to type in. */}
                <Input
                  label="Custom alias"
                  placeholder="spring-sale"
                  autoComplete="off"
                  spellCheck="false"
                  value={customAlias}
                  onChange={(event) => setCustomAlias(event.target.value)}
                  error={fieldErrors.customAlias}
                  hint={aliasHint(customAlias)}
                />
              </div>
            </Card>

            <Card>
              <CardHeader
                title="Access"
                description="Optional limits on who can use the link, and for how long."
              />

              <div className="mt-6 space-y-6">
                <Input
                  label="Expiration date"
                  type="datetime-local"
                  value={expiresAt}
                  onChange={(event) => setExpiresAt(event.target.value)}
                  error={fieldErrors.expiresAt}
                  hint="Optional. After this moment the link stops resolving."
                />

                <div className="rounded-xl border border-ink-200 p-4 transition-colors hover:border-ink-300 sm:p-5">
                  <Toggle
                    checked={passwordEnabled}
                    onChange={(next) => {
                      setPasswordEnabled(next);
                      // Drop both the value and its error when switching off, so
                      // re-enabling starts clean rather than showing a stale
                      // complaint about a field that was just hidden.
                      if (!next) {
                        setPassword('');
                        setFieldErrors((current) => {
                          const { password: _removed, ...rest } = current;
                          return rest;
                        });
                      }
                    }}
                    label="Protect this link with a password"
                    description="Visitors must enter the password before they are redirected."
                  />

                  {passwordEnabled && (
                    <div className="mt-5 animate-slide-up border-t border-ink-100 pt-5">
                      <PasswordInput
                        label="Password"
                        placeholder="At least 4 characters"
                        value={password}
                        onChange={(event) => setPassword(event.target.value)}
                        error={fieldErrors.password}
                        maxLength={72}
                      />

                      <p className="mt-3 flex items-start gap-2 text-xs leading-relaxed text-ink-500">
                        <IconLock className="mt-px h-3.5 w-3.5 shrink-0 text-ink-400" />
                        Hashed with bcrypt before it is saved — it is never stored in
                        plain text and cannot be recovered, so keep a copy of it.
                      </p>
                    </div>
                  )}
                </div>
              </div>
            </Card>

            {/* The single CTA. Full width on small screens where a right-aligned
                button is easy to miss under a long form. */}
            <div className="flex flex-col gap-3 sm:flex-row-reverse sm:items-center">
              <Button
                type="submit"
                size="lg"
                loading={submitting}
                className="w-full sm:w-auto"
              >
                {submitting ? 'Generating…' : 'Generate short link'}
              </Button>

              <p className="text-sm text-ink-500 sm:mr-auto">
                Only the destination URL is required.
              </p>
            </div>
          </div>

          {/* ---------------- Live QR preview ---------------- */}
          {/* Sticky only where there is a second column to be sticky in; stacked
              below xl it would just detach from the flow. */}
          <Card className="xl:sticky xl:top-8">
            <CardHeader
              title="QR code"
              description="Updates as you type. Colours and size apply to the download too."
            />

            <div className="mt-6">
              <QrPreview
                value={previewValue}
                caption={previewCaption}
                foreground={foreground}
                background={background}
                size={qrSize}
                onForegroundChange={setForeground}
                onBackgroundChange={setBackground}
                onSizeChange={setQrSize}
              />
            </div>
          </Card>
        </div>
      </form>
    </>
  );
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

/**
 * Prepends a scheme when one is missing.
 *
 * A bare domain is what people actually type, and the backend's pattern rejects it.
 * Resolving it here saves a validation error for something unambiguous.
 */
function normaliseUrl(value) {
  return /^[a-z][a-z0-9+.-]*:\/\//i.test(value) ? value : `https://${value}`;
}

/** Cheap structural check; the server's pattern remains the authority. */
function isProbablyUrl(value) {
  try {
    const parsed = new URL(value);
    // Requires a dot in the host: "https://foo" parses fine but is not something
    // anyone can visit, and catching it here is friendlier than a 400.
    return Boolean(parsed.hostname) && parsed.hostname.includes('.');
  } catch {
    return false;
  }
}

/** Shows the address an alias will claim, once it is one that could be claimed. */
function aliasHint(alias) {
  const trimmed = alias.trim();
  if (!trimmed) return 'Optional. Leave empty and a short code is generated for you.';
  if (!ALIAS_PATTERN.test(trimmed)) return 'Optional. Leave empty and a short code is generated for you.';

  return `Your link will be ${shortLinkOrigin().replace(/^https?:\/\//, '')}/${trimmed}`;
}

/**
 * Folds a failed request back into the form.
 *
 * Field-level errors are shown against their field. A 409 is always an alias
 * conflict — the only unique constraint a client can collide with — and arrives
 * without a field map, so it is routed to the alias input by hand rather than
 * appearing as a toast disconnected from the input that caused it.
 */
function applyServerError(error, setFieldErrors, toast) {
  if (error instanceof ApiError && error.fieldErrors) {
    setFieldErrors(error.fieldErrors);
    return;
  }

  const message = error?.message ?? 'Could not create the link';

  if (error?.status === 409) {
    setFieldErrors({ customAlias: message });
  } else if (error?.status === 400) {
    setFieldErrors({ originalUrl: message });
  } else {
    toast.error(message);
  }
}
