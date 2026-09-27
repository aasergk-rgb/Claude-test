import { useEffect, useState } from 'react';

export type Route = 'dashboard' | 'history' | 'settings' | 'paywall';

const ROUTES: Route[] = ['dashboard', 'history', 'settings', 'paywall'];

function parse(): Route {
  const r = location.hash.replace(/^#\/?/, '') as Route;
  return ROUTES.includes(r) ? r : 'dashboard';
}

export function navigate(route: Route) {
  const hash = route === 'dashboard' ? '#/' : `#/${route}`;
  if (location.hash !== hash) location.hash = hash;
}

export function useRoute(): Route {
  const [route, setRoute] = useState(parse);
  useEffect(() => {
    const on = () => {
      setRoute(parse());
      window.scrollTo(0, 0);
    };
    window.addEventListener('hashchange', on);
    return () => window.removeEventListener('hashchange', on);
  }, []);
  return route;
}
