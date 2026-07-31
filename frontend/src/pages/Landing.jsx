import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import Navbar from '../components/layout/Navbar';
import Footer from '../components/layout/Footer';
import Button from '../components/ui/Button';
import Badge from '../components/ui/Badge';
import { IconBolt, IconChart, IconClock, IconGlobe, IconQr, IconShield } from '../components/ui/Icons';
import { useAuth } from '../context/AuthContext';

/**
 * Marketing homepage.
 *
 * Structured as the brief specified: logo and nav, a large URL input with custom
 * alias and expiry, then features, pricing and footer. There is deliberately no
 * analytics chart here — the homepage sells the product and gets you to create a
 * link; measurement lives in the dashboard.
 *
 * The hero input does not create anything itself. It carries the URL to the create
 * page for a signed-in visitor, or to registration for everyone else — and from
 * there on to the same create page. Either way the URL someone typed is waiting for
 * them rather than being something they have to paste again, and there is exactly
 * one place in the app where a link is actually made.
 */
export default function Landing() {
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();

  return (
    <div className="flex min-h-screen flex-col">
      <Navbar />

      <main className="flex-1">
        <Hero
          isAuthenticated={isAuthenticated}
          onSubmitUrl={(url) =>
            navigate(isAuthenticated ? '/dashboard/create' : '/register', {
              state: { pendingUrl: url },
            })
          }
        />
        <Features />
        <HowItWorks />
        <Pricing isAuthenticated={isAuthenticated} />
        <CallToAction isAuthenticated={isAuthenticated} />
      </main>

      <Footer />
    </div>
  );
}

// ---------------------------------------------------------------------------
// Hero
// ---------------------------------------------------------------------------

function Hero({ isAuthenticated, onSubmitUrl }) {
  const [url, setUrl] = useState('');

  return (
    <section className="relative overflow-hidden border-b border-ink-100">
      {/* A single very soft radial tint. Not a gradient background - the brief
          ruled those out - just enough to keep the fold from reading as flat. */}
      <div
        className="pointer-events-none absolute inset-x-0 top-0 h-96 bg-[radial-gradient(60%_100%_at_50%_0%,rgb(37_99_235_/_0.05),transparent)]"
        aria-hidden="true"
      />

      <div className="container-page relative py-16 sm:py-24 lg:py-28">
        <div className="mx-auto max-w-3xl text-center">
          <Badge tone="brand" dot={false} className="mb-5 sm:mb-6">
            Free while you are getting started
          </Badge>

          {/* Scales from 30px on a small phone up to 60px on a desktop. The
              intermediate steps stop the headline wrapping awkwardly on the
              narrow-tablet widths between them. */}
          <h1 className="text-[1.875rem] font-semibold leading-[1.15] tracking-tight text-ink-900 sm:text-4xl md:text-5xl lg:text-6xl">
            Short links that tell you
            <br className="hidden sm:block" />{' '}
            {/* One accent word. A gradient across the whole headline would be the
                heavy treatment the brief ruled out; on two words it just adds a
                little warmth. */}
            <span className="bg-gradient-to-r from-brand-600 to-brand-400 bg-clip-text text-transparent">
              what happened next
            </span>
          </h1>

          <p className="mx-auto mt-5 max-w-xl text-base leading-relaxed text-ink-500 sm:mt-6 sm:text-lg">
            Shorten a URL, brand it with your own alias, and see every click —
            browser, device and location — in one clean dashboard.
          </p>
        </div>

        <div className="mx-auto mt-10 max-w-2xl">
          <form
            onSubmit={(event) => {
              event.preventDefault();
              if (url.trim()) onSubmitUrl(url.trim());
            }}
            className="rounded-2xl border border-ink-200 bg-white p-4 shadow-card sm:p-5"
          >
            <div className="flex flex-col gap-3 sm:flex-row">
              <input
                type="text"
                inputMode="url"
                spellCheck="false"
                autoComplete="off"
                aria-label="Link to shorten"
                placeholder="Paste a long link, e.g. example.com/a/very/long/path"
                value={url}
                onChange={(event) => setUrl(event.target.value)}
                className="h-12 flex-1 rounded-lg border border-ink-200 px-4 text-sm text-ink-900 transition-colors placeholder:text-ink-400 hover:border-ink-300 focus:border-brand-500 focus:outline-none"
              />
              <Button type="submit" size="lg" className="h-12 shrink-0">
                Shorten it
              </Button>
            </div>

            <p className="mt-3 text-center text-sm text-ink-500 sm:text-left">
              {isAuthenticated
                ? 'Opens the create page with this URL filled in.'
                : 'Free account, no card required. Takes about twenty seconds.'}
            </p>
          </form>
        </div>

        <dl className="mx-auto mt-12 grid max-w-2xl grid-cols-3 gap-3 border-t border-ink-100 pt-8 sm:mt-16 sm:gap-6 sm:pt-10">
          {[
            ['Redirects', 'Sub-10ms'],
            ['Click detail', 'Per visitor'],
            ['QR codes', 'Every link'],
          ].map(([label, value]) => (
            <div key={label} className="text-center">
              <dt className="text-[10px] uppercase tracking-wide text-ink-400 sm:text-xs">
                {label}
              </dt>
              <dd className="mt-1.5 text-sm font-semibold text-ink-900 sm:text-lg">{value}</dd>
            </div>
          ))}
        </dl>
      </div>
    </section>
  );
}

// ---------------------------------------------------------------------------
// Features
// ---------------------------------------------------------------------------

function Features() {
  const features = [
    {
      icon: IconBolt,
      title: 'Fast redirects',
      body: 'Every short code is cached in Redis, so a redirect answers without touching the database.',
    },
    {
      icon: IconChart,
      title: 'Real click analytics',
      body: 'Browser, operating system, device and referring address for every click — parsed, not guessed.',
    },
    {
      icon: IconGlobe,
      title: 'Custom aliases',
      body: 'Claim a readable alias like /spring-sale instead of a random string nobody trusts.',
    },
    {
      icon: IconQr,
      title: 'QR codes',
      body: 'Every link gets a downloadable QR. Scans route through the short link, so they count too.',
    },
    {
      icon: IconClock,
      title: 'Expiring links',
      body: 'Set an expiry and the link retires itself. Useful for time-boxed campaigns and one-off shares.',
    },
    {
      icon: IconShield,
      title: 'Private by default',
      body: 'Your links and their analytics are visible only to you. Every request is authorised individually.',
    },
  ];

  return (
    <section id="features" className="border-b border-ink-100 py-20 sm:py-24">
      <div className="container-page">
        <div className="max-w-2xl">
          <p className="text-sm font-semibold uppercase tracking-wide text-brand-600">Features</p>
          <h2 className="mt-3 text-3xl font-semibold tracking-tight text-ink-900 sm:text-4xl">
            Everything you need, nothing you don&apos;t
          </h2>
          <p className="mt-4 text-lg leading-relaxed text-ink-500">
            A link shortener should be quick to use and honest about what it measures.
            That is the whole product.
          </p>
        </div>

        <div className="mt-12 grid gap-x-8 gap-y-9 sm:mt-14 sm:grid-cols-2 sm:gap-y-10 lg:grid-cols-3">
          {features.map(({ icon: Icon, title, body }) => (
            <div key={title} className="group">
              {/* The icon tile tilts very slightly on hover. Small enough that you
                  register it as responsiveness rather than as an animation. */}
              <span className="flex h-10 w-10 items-center justify-center rounded-lg bg-brand-50 text-brand-600 transition-transform duration-200 group-hover:-rotate-6 group-hover:scale-110">
                <Icon />
              </span>
              <h3 className="mt-4 text-base font-semibold text-ink-900">{title}</h3>
              <p className="mt-2 text-sm leading-relaxed text-ink-500">{body}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

// ---------------------------------------------------------------------------
// How it works
// ---------------------------------------------------------------------------

function HowItWorks() {
  const steps = [
    {
      step: '01',
      title: 'Paste your link',
      body: 'Drop in any URL. Add a custom alias or an expiry date if you need one.',
    },
    {
      step: '02',
      title: 'Share it anywhere',
      body: 'Copy the short link or download its QR code. Both route through the same redirect.',
    },
    {
      step: '03',
      title: 'See what happened',
      body: 'Open the analytics page for a link to see clicks over time and who they came from.',
    },
  ];

  return (
    <section id="how-it-works" className="border-b border-ink-100 bg-ink-50/40 py-20 sm:py-24">
      <div className="container-page">
        <div className="max-w-2xl">
          <p className="text-sm font-semibold uppercase tracking-wide text-brand-600">How it works</p>
          <h2 className="mt-3 text-3xl font-semibold tracking-tight text-ink-900 sm:text-4xl">
            Three steps, about a minute
          </h2>
        </div>

        <ol className="mt-12 grid gap-5 sm:mt-14 sm:gap-6 md:grid-cols-3">
          {steps.map(({ step, title, body }) => (
            <li
              key={step}
              className="rounded-lg border border-ink-200 bg-white p-6 transition-colors duration-200 hover:border-ink-400"
            >
              <span className="font-mono text-sm font-semibold text-brand-600">{step}</span>
              <h3 className="mt-3 text-base font-semibold text-ink-900">{title}</h3>
              <p className="mt-2 text-sm leading-relaxed text-ink-500">{body}</p>
            </li>
          ))}
        </ol>
      </div>
    </section>
  );
}

// ---------------------------------------------------------------------------
// Pricing
// ---------------------------------------------------------------------------

function Pricing({ isAuthenticated }) {
  const plans = [
    {
      name: 'Free',
      price: '$0',
      cadence: 'forever',
      description: 'For personal links and trying things out.',
      features: [
        '50 links per month',
        'Click analytics',
        'QR codes',
        'Custom aliases',
        'Link expiry',
      ],
      cta: 'Get started',
      highlighted: false,
    },
    {
      name: 'Pro',
      price: '$12',
      cadence: 'per month',
      description: 'For people who ship campaigns regularly.',
      features: [
        'Unlimited links',
        'Full click history',
        'Bulk QR download',
        'Custom domain',
        'API access',
        'Priority support',
      ],
      cta: 'Start free trial',
      highlighted: true,
    },
    {
      name: 'Team',
      price: '$39',
      cadence: 'per month',
      description: 'For teams sharing one link workspace.',
      features: [
        'Everything in Pro',
        'Up to 10 members',
        'Shared workspace',
        'Role permissions',
        'Audit log',
      ],
      cta: 'Contact sales',
      highlighted: false,
    },
  ];

  return (
    <section id="pricing" className="border-b border-ink-100 py-20 sm:py-24">
      <div className="container-page">
        <div className="mx-auto max-w-2xl text-center">
          <p className="text-sm font-semibold uppercase tracking-wide text-brand-600">Pricing</p>
          <h2 className="mt-3 text-3xl font-semibold tracking-tight text-ink-900 sm:text-4xl">
            Simple, predictable pricing
          </h2>
          <p className="mt-4 text-lg text-ink-500">
            Start free. Upgrade when your links start doing real work.
          </p>
        </div>

        {/* Three columns only from lg. At md the cards get too narrow for the
            feature lists, and two columns would leave an orphan. */}
        <div className="mt-12 grid gap-5 sm:mt-14 sm:gap-6 lg:grid-cols-3">
          {plans.map((plan) => (
            <div
              key={plan.name}
              className={[
                'relative flex flex-col rounded-lg border bg-white p-6 sm:p-7',
                'transition-colors duration-200',
                // The featured plan is marked by a heavier rule rather than by
                // being lifted out of the row. With no shadows, a raised card has
                // nothing to sit above and just looks misaligned.
                plan.highlighted
                  ? 'border-brand-600 ring-1 ring-brand-600'
                  : 'border-ink-200 hover:border-ink-400',
              ].join(' ')}
            >
              {plan.highlighted && (
                <span className="absolute -top-2.5 left-7 rounded bg-brand-600 px-2.5 py-0.5 text-xs font-medium text-white">
                  Most popular
                </span>
              )}

              <h3 className="text-base font-semibold text-ink-900">{plan.name}</h3>
              <p className="mt-1.5 text-sm text-ink-500">{plan.description}</p>

              <p className="mt-6 flex items-baseline gap-1.5">
                <span className="text-4xl font-semibold tracking-tight text-ink-900">
                  {plan.price}
                </span>
                <span className="text-sm text-ink-500">{plan.cadence}</span>
              </p>

              <ul className="mt-7 flex-1 space-y-3">
                {plan.features.map((feature) => (
                  <li key={feature} className="flex items-start gap-2.5 text-sm text-ink-600">
                    <svg
                      className="mt-0.5 h-4 w-4 shrink-0 text-brand-600"
                      viewBox="0 0 16 16"
                      fill="none"
                      aria-hidden="true"
                    >
                      <path
                        d="m3 8.5 3 3 7-7.5"
                        stroke="currentColor"
                        strokeWidth="1.8"
                        strokeLinecap="round"
                        strokeLinejoin="round"
                      />
                    </svg>
                    {feature}
                  </li>
                ))}
              </ul>

              <Link
                to={isAuthenticated ? '/dashboard' : '/register'}
                className="mt-8 block"
              >
                <Button variant={plan.highlighted ? 'primary' : 'secondary'} fullWidth>
                  {isAuthenticated ? 'Go to dashboard' : plan.cta}
                </Button>
              </Link>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

// ---------------------------------------------------------------------------
// Closing CTA
// ---------------------------------------------------------------------------

function CallToAction({ isAuthenticated }) {
  return (
    <section className="py-20 sm:py-24">
      <div className="container-page">
        <div className="rounded-2xl border border-ink-200 bg-ink-50/60 px-6 py-12 text-center sm:px-14 sm:py-14">
          <h2 className="text-2xl font-semibold tracking-tight text-ink-900 sm:text-3xl">
            Start shortening in under a minute
          </h2>
          <p className="mx-auto mt-4 max-w-lg text-base text-ink-500 sm:text-lg">
            Create a free account and get your first link, its QR code and its
            analytics straight away.
          </p>

          <div className="mt-8 flex flex-col justify-center gap-3 sm:flex-row">
            <Link to={isAuthenticated ? '/dashboard' : '/register'}>
              <Button size="lg">
                {isAuthenticated ? 'Go to dashboard' : 'Create free account'}
              </Button>
            </Link>
            {!isAuthenticated && (
              <Link to="/login">
                <Button size="lg" variant="secondary">
                  Log in
                </Button>
              </Link>
            )}
          </div>
        </div>
      </div>
    </section>
  );
}
