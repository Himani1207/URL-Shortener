import { Link } from 'react-router-dom';
import Logo from '../ui/Logo';

/**
 * Marketing footer.
 *
 * Deliberately restrained — four short columns rather than the sprawling link
 * farm that makes a small product look like it is pretending to be a large one.
 */
export default function Footer() {
  const columns = [
    {
      title: 'Product',
      links: [
        { label: 'Features', href: '#features' },
        { label: 'Pricing', href: '#pricing' },
        { label: 'How it works', href: '#how-it-works' },
      ],
    },
    {
      title: 'Developers',
      links: [
        { label: 'API documentation', href: '/swagger-ui.html', external: true },
        { label: 'Status', href: '/actuator/health', external: true },
      ],
    },
    {
      title: 'Company',
      links: [
        { label: 'About', href: '#' },
        { label: 'Contact', href: '#' },
      ],
    },
  ];

  return (
    <footer className="border-t border-ink-100 bg-ink-50/50">
      <div className="container-page py-14">
        <div className="grid gap-10 sm:grid-cols-2 lg:grid-cols-5">
          <div className="lg:col-span-2">
            <Logo />
            <p className="mt-4 max-w-xs text-sm leading-relaxed text-ink-500">
              Short links, click analytics and QR codes. Built for teams who want to
              know what happens after someone clicks.
            </p>
          </div>

          {columns.map((column) => (
            <div key={column.title}>
              <h3 className="text-sm font-semibold text-ink-900">{column.title}</h3>
              <ul className="mt-4 space-y-3">
                {column.links.map((link) => (
                  <li key={link.label}>
                    {link.external ? (
                      <a
                        href={link.href}
                        target="_blank"
                        rel="noreferrer"
                        className="rounded text-sm text-ink-500 transition-colors hover:text-ink-900"
                      >
                        {link.label}
                      </a>
                    ) : (
                      <a
                        href={link.href}
                        className="rounded text-sm text-ink-500 transition-colors hover:text-ink-900"
                      >
                        {link.label}
                      </a>
                    )}
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>

        <div className="mt-12 flex flex-col items-center justify-between gap-4 border-t border-ink-200 pt-8 sm:flex-row">
          <p className="text-sm text-ink-500">
            © {new Date().getFullYear()} Linkly. All rights reserved.
          </p>
          <div className="flex gap-6">
            <Link to="/login" className="rounded text-sm text-ink-500 transition-colors hover:text-ink-900">
              Log in
            </Link>
            <Link to="/register" className="rounded text-sm text-ink-500 transition-colors hover:text-ink-900">
              Create account
            </Link>
          </div>
        </div>
      </div>
    </footer>
  );
}
