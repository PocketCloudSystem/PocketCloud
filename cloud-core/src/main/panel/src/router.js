import { useEffect, useState } from 'preact/hooks';

// hash routing: the cloud only serves index.html at <prefix>/panel/, and reloads keep working
const current = () => location.hash.replace(/^#/, '') || '/';

export function useRoute() {
  const [route, setRoute] = useState(current);
  useEffect(() => {
    const update = () => setRoute(current());
    addEventListener('hashchange', update);
    return () => removeEventListener('hashchange', update);
  }, []);
  return route;
}

export const navigate = (to) => { location.hash = to; };
