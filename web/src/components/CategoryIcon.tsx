import type { CSSProperties } from 'react';
import { getCategory } from '../lib/categories';
import { Icon } from './Icon';

export function CategoryIcon({ id, small }: { id: string; small?: boolean }) {
  const c = getCategory(id);
  return (
    <span className={small ? 'cat-ico sm' : 'cat-ico'} style={{ '--c': c.color } as CSSProperties}>
      <Icon name={`c-${c.id}`} />
    </span>
  );
}
